/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.xm.collision;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

import grondag.fermion.bits.BitHelper;
import grondag.xm.collision.Functions.AreaBoundsIntFunction;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Static utility methods for BoxFinder
 */
@API(status = INTERNAL)
class BoxFinderUtils {
    static final long[] AREAS;
    static final int[] VOLUME_KEYS;
    static final int VOLUME_COUNT;
    static final short[] BOUNDS;

    static final long[] EMPTY = new long[64];

    static final Slice[][] lookupMinMax = new Slice[8][8];

    /**
     * How many volumes are at least 4 voxels in volume. Will come first in
     * {@link #VOLUME_KEYS}
     */
    static final int VOLUME_COUNT_4_PLUS;

    /**
     * Max is inclusive and equal to max attribute of slice.
     */
    static Slice sliceByMinMax(int minZ, int maxZ) {
        return lookupMinMax[minZ][maxZ];
    }

    /**
     * All possible contiguous sections of the Z-axis into 1/8 units.
     * <p>
     * 
     * In retrospect, would probably be better if Slice were simply an 8-bit word
     * instead of an enum but code is working and is "fast enough" for now. Could
     * matter, tho, with LOR.
     */
    static enum Slice {
        D1_0(1, 0), D1_1(1, 1), D1_2(1, 2), D1_3(1, 3), D1_4(1, 4), D1_5(1, 5), D1_6(1, 6), D1_7(1, 7), D2_0(2, 0), D2_1(2, 1), D2_2(2, 2), D2_3(2, 3),
        D2_4(2, 4), D2_5(2, 5), D2_6(2, 6), D3_0(3, 0), D3_1(3, 1), D3_2(3, 2), D3_3(3, 3), D3_4(3, 4), D3_5(3, 5), D4_0(4, 0), D4_1(4, 1), D4_2(4, 2),
        D4_3(4, 3), D4_4(4, 4), D5_0(5, 0), D5_1(5, 1), D5_2(5, 2), D5_3(5, 3), D6_0(6, 0), D6_1(6, 1), D6_2(6, 2), D7_0(7, 0), D7_1(7, 1), D8_0(8, 0);

        final int depth;
        final int min;

        /**
         * INCLUSIVE
         */
        final int max;

        /**
         * Bits are set if Z layer is included. Used for fast intersection testing.
         */
        final int layerBits;

        private Slice(int depth, int min) {
            this.depth = depth;
            this.min = min;
            this.max = min + depth - 1;

            int flags = 0;
            for (int i = 0; i < depth; i++) {
                flags |= (1 << (min + i));
            }

            this.layerBits = flags;
        }
    }

    private static final Slice[] SLICES = Slice.values();
    static final int SLICE_COUNT = SLICES.length;

    static {
        for (Slice slice : SLICES) {
            lookupMinMax[slice.min][slice.max] = slice;
        }

        LongOpenHashSet patterns = new LongOpenHashSet();

        for (int xSize = 1; xSize <= 8; xSize++) {
            for (int ySize = 1; ySize <= 8; ySize++) {
                addPatterns(xSize, ySize, patterns);
            }
        }

        AREAS = patterns.toLongArray();

        LongArrays.quickSort(AREAS, new LongComparator() {
            @Override
            public int compare(Long o1, Long o2) {
                return compare(o1.longValue(), o2.longValue());
            }

            @Override
            public int compare(long k1, long k2) {
                // note reverse order, want largest first
                return Integer.compare(Long.bitCount(k2), Long.bitCount(k1));
            }
        });

        BOUNDS = new short[AREAS.length];
        for (int i = 0; i < AREAS.length; i++) {
            long pattern = AREAS[i];
            long xBits = pattern | (pattern >>> 32);
            xBits |= xBits >>> 16;
            xBits |= xBits >>> 8;
            xBits &= 0xFFL;
            BOUNDS[i] = (short) (minX(xBits) | (maxX(xBits) << 3) | (minY(pattern) << 6) | (maxY(pattern) << 9));
        }

        IntArrayList volumes = new IntArrayList();
        for (Slice slice : Slice.values()) {
            for (int i = 0; i < AREAS.length; i++) {
                if (slice.depth * Long.bitCount(AREAS[i]) > 1)
                    volumes.add(volumeKey(slice, i));
            }
        }

        VOLUME_KEYS = volumes.toIntArray();
        IntArrays.quickSort(BoxFinderUtils.VOLUME_KEYS, new IntComparator() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return compare(o1.intValue(), o2.intValue());
            }

            @Override
            public int compare(int k1, int k2) {
                // note reverse order, want largest first
                return Integer.compare(k2, k1);
            }
        });

        VOLUME_COUNT = VOLUME_KEYS.length;

        int countFourPlus = 0;
        for (int i = 0; i < VOLUME_COUNT; i++) {
            if (volumeFromKey(VOLUME_KEYS[i]) < 4) {
                if (countFourPlus == 0)
                    countFourPlus = i;
            } else
                assert countFourPlus == 0 : "volumes not in volume descending order";
        }
        VOLUME_COUNT_4_PLUS = countFourPlus;
    }

    static void findBestExclusionBits() {
        final IntArrayList[] VOLUMES_BY_BIT = new IntArrayList[512];
        final int[] COUNTS_BY_BIT = new int[512];
        int coverageCount = 0;

        for (int i = 0; i < 512; i++) {
            VOLUMES_BY_BIT[i] = new IntArrayList();
        }

        for (int v = 0; v < VOLUME_KEYS.length; v++) {
            final int k = VOLUME_KEYS[v];
            final Slice slice = sliceFromKey(k);
            final long pattern = patternFromKey(k);
            final int vFinal = v;

            BitHelper.forEachBit(pattern, i -> {
                final int x = xFromAreaBitIndex(i);
                final int y = yFromAreaBitIndex(i);
                for (int z = slice.min; z <= slice.max; z++) {
                    final int n = x | (y << 3) | (z << 6);
                    VOLUMES_BY_BIT[n].add(vFinal);
                    COUNTS_BY_BIT[n]++;
                }
            });
        }

        boolean coverage[] = new boolean[VOLUME_KEYS.length];

        int firstIndex = -1;
        int bestCount = -1;
        for (int i = 0; i < 512; i++) {
            if (COUNTS_BY_BIT[i] > bestCount) {
                bestCount = COUNTS_BY_BIT[i];
                firstIndex = i;
            }
        }

        coverageCount += bestCount;
        System.out.println("First bit coverage  = " + bestCount);

        for (int v : VOLUMES_BY_BIT[firstIndex])
            coverage[v] = true;

        int secondIndex = -1;
        bestCount = -1;
        for (int i = 0; i < 512; i++) {
            if (i == firstIndex)
                continue;

            if (COUNTS_BY_BIT[i] > bestCount) {
                int c = 0;
                for (int j : VOLUMES_BY_BIT[i])
                    if (!coverage[j])
                        c++;

                if (c > bestCount) {
                    bestCount = c;
                    secondIndex = i;
                }
            }

            for (int v : VOLUMES_BY_BIT[secondIndex])
                coverage[v] = true;
        }

        coverageCount += bestCount;
        System.out.println("Second bit coverage  = " + bestCount);

        int thirdIndex = -1;
        bestCount = -1;
        for (int i = 0; i < 512; i++) {
            if (i == firstIndex || i == secondIndex)
                continue;

            if (COUNTS_BY_BIT[i] > bestCount) {
                int c = 0;
                for (int j : VOLUMES_BY_BIT[i])
                    if (!coverage[j])
                        c++;

                if (c > bestCount) {
                    bestCount = c;
                    thirdIndex = i;
                }
            }

            for (int v : VOLUMES_BY_BIT[thirdIndex])
                coverage[v] = true;
        }

        coverageCount += bestCount;
        System.out.println("Third bit coverage  = " + bestCount);

        int fourthIndex = -1;
        bestCount = -1;
        for (int i = 0; i < 512; i++) {
            if (i == firstIndex || i == secondIndex)
                continue;

            if (COUNTS_BY_BIT[i] > bestCount) {
                int c = 0;
                for (int j : VOLUMES_BY_BIT[i])
                    if (!coverage[j])
                        c++;

                if (c > bestCount) {
                    bestCount = c;
                    fourthIndex = i;
                }
            }

            for (int v : VOLUMES_BY_BIT[fourthIndex])
                coverage[v] = true;
        }

        coverageCount += bestCount;
        System.out.println("Fourth bit coverage  = " + bestCount);

        System.out.println("Coverge % = " + 100 * coverageCount / VOLUME_KEYS.length);
    }

    /**
     * Assumes values are pre-sorted.
     */
    static int intersectIndexUnsafe(int high, int low) {
        return high * (high - 1) / 2;
    }

    /**
     * Returns the number of maximal volume that target volume must be split into if
     * the actorVolume is chosen for output. Note this total does not count boxes
     * that would be included in the output volume.
     * <p>
     * 
     * Returns 0 if the boxes do not intersect.
     * <p>
     * 
     * Computed as the sum of actor bounds (any axis or side) that are within (not
     * on the edge) of the target volume. This works because each face within bounds
     * will force a split of the target box along the plane of the face.
     * 
     * We subtract one from this total because we aren't counting the boxes that
     * would be absorbed by the actor volume.
     */
    static int splitScore(int actorVolIndex, int targetVolIndex) {
        final Slice actorSlice = sliceFromKey(actorVolIndex);
        final Slice targetSlice = sliceFromKey(targetVolIndex);

        int result = 0;

        // Must be >= or <= on one side of comparison because indexes are voxels and
        // actual face depends on usage (min/max)

        if (actorSlice.min > targetSlice.min && actorSlice.min <= targetSlice.max)
            result++;

        if (actorSlice.max >= targetSlice.min && actorSlice.max < targetSlice.max)
            result++;

        result += testAreaBounds(patternIndexFromKey(targetVolIndex), (targetMinX, targetMinY, targetMaxX, targetMaxY) -> {
            return testAreaBounds(patternIndexFromKey(actorVolIndex), (actorMinX, actorMinY, actorMaxX, actorMaxY) -> {
                int n = 0;
                if (actorMinX > targetMinX && actorMinX <= targetMaxX)
                    n++;

                if (actorMaxX >= targetMinX && actorMaxX < targetMaxX)
                    n++;

                if (actorMinY > targetMinY && actorMinY <= targetMaxY)
                    n++;

                if (actorMaxY >= targetMinY && actorMaxY < targetMaxY)
                    n++;

                return n;
            });
        });

        return result == 0 ? 0 : result - 1;
    }

    /**
     * Validates ordering and sorts if needed.
     */
    static int intersectIndex(int a, int b) {
        return a > b ? intersectIndexUnsafe(a, b) : intersectIndexUnsafe(b, a);
    }

    private static void addPatterns(int xSize, int ySize, LongOpenHashSet patterns) {
        for (int xOrigin = 0; xOrigin <= 8 - xSize; xOrigin++) {
            for (int yOrigin = 0; yOrigin <= 8 - ySize; yOrigin++) {
                long pattern = makePattern(xOrigin, yOrigin, xSize, ySize);

//                if(yOrigin + ySize < 8)
//                    assert ((pattern << 8) | pattern) == makePattern(xOrigin, yOrigin, xSize, ySize + 1);
//                
//                if(yOrigin > 0)
//                    assert ((pattern >>> 8) | pattern) == makePattern(xOrigin, yOrigin - 1, xSize, ySize + 1);
//                
//                final int x0 = xOrigin;
//                final int y0 = yOrigin;
//                final int x1 = xOrigin + xSize - 1;
//                final int y1 = yOrigin + ySize - 1;
//                testAreaBounds(pattern, (minX, minY, maxX, maxY) ->
//                {
//                    assert minX == x0;
//                    assert minY == y0;
//                    assert maxX == x1;
//                    assert maxY == y1;
//                    return 0;
//                });

                patterns.add(pattern);
            }
        }
    }

    static long makePattern(int xOrigin, int yOrigin, int xSize, int ySize) {
        long pattern = 0;
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                pattern |= (1L << areaBitIndex(xOrigin + x, yOrigin + y));
            }
        }
        return pattern;
    }

    static int areaBitIndex(int x, int y) {
        return x | (y << 3);
    }

    static int xFromAreaBitIndex(int bitIndex) {
        return bitIndex & 7;
    }

    static int yFromAreaBitIndex(int bitIndex) {
        return (bitIndex >>> 3) & 7;
    }

    /**
     * Single-pass lookup of x, y bounds for given area index.
     */
    static int testAreaBounds(int areaIndex, AreaBoundsIntFunction test) {
        final int bounds = BOUNDS[areaIndex];

        return test.apply(bounds & 7, (bounds >> 6) & 7, (bounds >> 3) & 7, (bounds >> 9) & 7);
    }

    /**
     * For testing.
     */
    static boolean doAreaBoundsMatch(int areaIndex, int minX, int minY, int maxX, int maxY) {
        return testAreaBounds(areaIndex, (x0, y0, x1, y1) -> {
            return (x0 == minX && y0 == minY && x1 == maxX && y1 == maxY) ? 1 : 0;
        }) == 1;
    }

    private static int minX(long xBits) {
        if ((xBits & 0b1111) == 0) {
            if ((xBits & 0b110000) == 0)
                return (xBits & 0b1000000) == 0 ? 7 : 6;
            else
                return (xBits & 0b10000) == 0 ? 5 : 4;
        } else {
            if ((xBits & 0b11) == 0)
                return (xBits & 0b0100) == 0 ? 3 : 2;
            else
                return (xBits & 0b1) == 0 ? 1 : 0;
        }
    }

    private static int minY(long yBits) {
        if ((yBits & 0xFFFFFFFFL) == 0L) {
            if ((yBits & 0xFFFFFFFFFFFFL) == 0L)
                return (yBits & 0xFFFFFFFFFFFFFFL) == 0L ? 7 : 6;
            else
                return (yBits & 0xFFFFFFFFFFL) == 0L ? 5 : 4;
        } else {
            if ((yBits & 0xFFFFL) == 0L)
                return (yBits & 0xFF0000L) == 0L ? 3 : 2;
            else
                return (yBits & 0xFFL) == 0L ? 1 : 0;
        }
    }

    private static int maxX(long xBits) {
        if ((xBits & 0b11110000) == 0) {
            if ((xBits & 0b1100) == 0)
                return (xBits & 0b10) == 0 ? 0 : 1;
            else
                return (xBits & 0b1000) == 0 ? 2 : 3;
        } else {
            if ((xBits & 0b11000000) == 0)
                return (xBits & 0b100000) == 0 ? 4 : 5;
            else
                return (xBits & 0b10000000) == 0 ? 6 : 7;
        }
    }

    private static int maxY(long yBits) {
        if ((yBits & 0xFFFFFFFF00000000L) == 0L) {
            if ((yBits & 0xFFFF0000L) == 0L)
                return (yBits & 0xFF00L) == 0L ? 0 : 1;
            else
                return (yBits & 0xFF000000) == 0L ? 2 : 3;
        } else {
            if ((yBits & 0xFFFF000000000000L) == 0L)
                return (yBits & 0xFF0000000000L) == 0L ? 4 : 5;
            else
                return (yBits & 0xFF00000000000000L) == 0L ? 6 : 7;
        }
    }

    /**
     * Encodes a volume key that is naturally sortable by volume. (Larger values
     * imply larger volume).
     */
    static int volumeKey(Slice slice, int patternIndex) {
        int volume = volume(slice, patternIndex);
        int result = (volume << 17) | (patternIndex << 6) | slice.ordinal();
        assert result > 0;
        return result;
    }

    static int volume(Slice slice, int patternIndex) {
        return slice.depth * Long.bitCount(AREAS[patternIndex]);
    }

    static int volumeFromKey(int volumeKey) {
        return (volumeKey >> 17);
    }

    static int patternIndexFromKey(int volumeKey) {
        return (volumeKey >> 6) & 2047;
    }

    static long patternFromKey(int volumeKey) {
        return AREAS[patternIndexFromKey(volumeKey)];
    }

    static Slice sliceFromKey(int volumeKey) {
        return SLICES[(volumeKey & 63)];
    }

    /**
     * True if volumes share any voxels, including case where one volume fully
     * includes the other.
     */
    static boolean doVolumesIntersect(int volumeKey0, int volumeKey1) {
        return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) != 0
                && (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) != 0L;
    }

    static boolean doesVolumeIncludeBit(int volumeKey, int x, int y, int z) {
        return (sliceFromKey(volumeKey).layerBits & (1 << z)) != 0 && (patternFromKey(volumeKey) & (1L << areaBitIndex(x, y))) != 0L;
    }

    /**
     * True if volume matches the given bounds.<br>
     * Second point coordinates are inclusive.
     */
    static boolean areVolumesSame(int volumeKey, int x0, int y0, int z0, int x1, int y1, int z1) {
        return sliceFromKey(volumeKey).min == z0 & sliceFromKey(volumeKey).max == z1 && doAreaBoundsMatch(patternIndexFromKey(volumeKey), x0, y0, x1, y1);
    }

    /**
     * True if volumes share no voxels.
     */
    static boolean areVolumesDisjoint(int volumeKey0, int volumeKey1) {
        return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) == 0
                || (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) == 0L;
    }

    /**
     * True if the "big" volume fully includes the "small" volume. False is the
     * volumes are the same volume, if "small" volume is actually larger, or if the
     * small volume contains any voxels not part of the big volume.
     */
    static boolean isVolumeIncluded(int bigKey, int smallKey) {
        // big volume must be larger than and distinct from the small volume
        if (volumeFromKey(bigKey) <= volumeFromKey(smallKey))
            return false;

        final int smallSliceBits = sliceFromKey(smallKey).layerBits;
        if ((sliceFromKey(bigKey).layerBits & smallSliceBits) != smallSliceBits)
            return false;

        final long smallPattern = patternFromKey(smallKey);
        return ((patternFromKey(bigKey) & smallPattern) == smallPattern);
    }

    /**
     * Returns number of voxels the exist in both of the given volumes, if any.
     */
    static int intersectingVoxelCount(int vol0, int vol1) {
        int sliceBits = sliceFromKey(vol0).layerBits & sliceFromKey(vol1).layerBits;
        if (sliceBits == 0)
            return 0;

        return Long.bitCount(sliceBits) * Long.bitCount(patternFromKey(vol0) & patternFromKey(vol1));
    }

    /**
     * Returns number of voxels the exist in minimum volume encompassing both given
     * volumes.
     */
    static int unionVoxelCount(int vol0, int vol1) {
        Slice s0 = sliceFromKey(vol0);
        Slice s1 = sliceFromKey(vol1);
        // +1 because max is inclusive
        int sliceBits = Math.max(s0.max, s1.max) - Math.min(s0.min, s1.min) + 1;

        int areaBits = testAreaBounds(patternIndexFromKey(vol0), (minX0, minY0, maxX0, maxY0) -> {
            return testAreaBounds(patternIndexFromKey(vol1), (minX1, minY1, maxX1, maxY1) -> {
                final int x = Math.max(maxX0, maxX1) - Math.min(minX0, minX1) + 1;
                final int y = Math.max(maxY0, maxY1) - Math.min(minY0, minY1) + 1;
                return x * y;
            });
        });

        return areaBits * sliceBits;
    }
}
