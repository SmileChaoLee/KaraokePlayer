// Copyright 2019 The libgav1 Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "src/dsp/warp.h"
#include "src/utils/cpu.h"

#if LIBGAV1_ENABLE_NEON

#include <arm_neon.h>

#include <algorithm>
#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <type_traits>

#include "src/dsp/arm/common_neon.h"
#include "src/dsp/constants.h"
#include "src/dsp/dsp.h"
#include "src/utils/common.h"
#include "src/utils/constants.h"

namespace libgav1 {
namespace dsp {
namespace low_bitdepth {
namespace {

// Number of extra bits of precision in warped filtering.
constexpr int kWarpedDiffPrecisionBits = 10;

// Applies the horizontal filter to one source row and stores the result in
// |intermediate_result_row|. |intermediate_result_row| is a row in the 15x8
// |intermediate_result| two-dimensional array.
void HorizontalFilter(const int sx4, const int16_t alpha,
                      const int horizontal_offset,
                      const int16x8_t src_row_low_s16,
                      const int16x8_t src_row_high_s16,
                      int16_t intermediate_result_row[8]) {
  int sx = sx4 - MultiplyBy4(alpha);
  int16x8_t filter[8];
  for (int x = 0; x < 8; ++x) {
    const int offset = RightShiftWithRounding(sx, kWarpedDiffPrecisionBits) +
                       kWarpedPixelPrecisionShifts;
    // TODO(wtc): kWarpedFilters fit in int8.
    filter[x] = vld1q_s16(kWarpedFilters[offset]);
    sx += alpha;
  }
  Transpose8x8(filter);
  // For 8 bit, the range of sum is within uint16_t, if we add a horizontal
  // offset. horizontal_offset guarantees sum is nonnegative.
  //
  // Proof:
  // Given that the minimum (most negative) sum of the negative filter
  // coefficients is -47 and the maximum sum of the positive filter
  // coefficients is 175, the range of the horizontal filter output is
  //   -47 * 255 <= output <= 175 * 255
  // Since -2^14 < -47 * 255, adding -2^14 (= horizontal_offset) to the
  // horizontal filter output produces a positive value:
  //   0 < output + 2^14 <= 175 * 255 + 2^14
  // The final rounding right shift by 3 (= kInterRoundBitsHorizontal) bits
  // adds 2^2 to the sum:
  //   0 < output + 2^14 + 2^2 <= 175 * 255 + 2^14 + 2^2 = 61013
  // Since 61013 < 2^16, the final sum (right before the right shift by 3
  // bits) will not overflow uint16_t. In addition, the value after the right
  // shift by 3 bits is in the following range:
  //   0 <= intermediate_result[y][x] < 2^13
  // This property is used in determining the range of the vertical filtering
  // output. [End of proof.]
  //
  // We can do signed int16_t arithmetic and just treat the final result as
  // uint16_t when we shift it right.
  int16x8_t sum = vdupq_n_s16(horizontal_offset);
  // Unrolled k = 0..7 loop. We need to manually unroll the loop because the
  // third argument (an index value) to vextq_s16() must be a constant
  // (immediate).
  // k = 0.
  int16x8_t src_row_v_s16 = src_row_low_s16;
  // TODO(wtc): use vmlal_s8 here.
  sum = vmlaq_s16(sum, filter[0], src_row_v_s16);
  // k = 1.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 1);
  sum = vmlaq_s16(sum, filter[1], src_row_v_s16);
  // k = 2.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 2);
  sum = vmlaq_s16(sum, filter[2], src_row_v_s16);
  // k = 3.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 3);
  sum = vmlaq_s16(sum, filter[3], src_row_v_s16);
  // k = 4.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 4);
  sum = vmlaq_s16(sum, filter[4], src_row_v_s16);
  // k = 5.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 5);
  sum = vmlaq_s16(sum, filter[5], src_row_v_s16);
  // k = 6.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 6);
  sum = vmlaq_s16(sum, filter[6], src_row_v_s16);
  // k = 7.
  src_row_v_s16 = vextq_s16(src_row_low_s16, src_row_high_s16, 7);
  sum = vmlaq_s16(sum, filter[7], src_row_v_s16);
  // End of unrolled k = 0..7 loop.
  // Treat sum as unsigned for the right shift.
  sum = vreinterpretq_s16_u16(
      vrshrq_n_u16(vreinterpretq_u16_s16(sum), kInterRoundBitsHorizontal));
  vst1q_s16(intermediate_result_row, sum);
}

template <bool clip>
void Warp_NEON(const void* const source, const ptrdiff_t source_stride,
               const int source_width, const int source_height,
               const int* const warp_params, const int subsampling_x,
               const int subsampling_y, const int inter_round_bits_vertical,
               const int block_start_x, const int block_start_y,
               const int block_width, const int block_height,
               const int16_t alpha, const int16_t beta, const int16_t gamma,
               const int16_t delta, void* dest, const ptrdiff_t dest_stride) {
  constexpr int bitdepth = 8;
  constexpr int kSingleRoundOffset = (1 << bitdepth) + (1 << (bitdepth - 1));
  union {
    // Intermediate_result is the output of the horizontal filtering and
    // rounding. The range is within 13 (= bitdepth + kFilterBits + 1 -
    // kInterRoundBitsHorizontal) bits (unsigned). We use the signed int16_t
    // type so that we can multiply it by kWarpedFilters (which has signed
    // values) using vmlal_s16().
    int16_t intermediate_result[15][8];  // 15 rows, 8 columns.
    // In the simple special cases where the samples in each row are all the
    // same, store one sample per row in a column vector.
    int16_t intermediate_result_column[15];
  };
  constexpr int horizontal_offset = 1 << (bitdepth + kFilterBits - 1);
  constexpr int vertical_offset =
      1 << (bitdepth + 2 * kFilterBits - kInterRoundBitsHorizontal);
  const auto* const src = static_cast<const uint8_t*>(source);
  using DestType = typename std::conditional<clip, uint8_t, uint16_t>::type;
  auto* dst = static_cast<DestType*>(dest);

  assert(block_width >= 8);
  assert(block_height >= 8);

  // Warp process applies for each 8x8 block (or smaller).
  int start_y = block_start_y;
  do {
    int start_x = block_start_x;
    do {
      const int src_x = (start_x + 4) << subsampling_x;
      const int src_y = (start_y + 4) << subsampling_y;
      const int dst_x =
          src_x * warp_params[2] + src_y * warp_params[3] + warp_params[0];
      const int dst_y =
          src_x * warp_params[4] + src_y * warp_params[5] + warp_params[1];
      const int x4 = dst_x >> subsampling_x;
      const int y4 = dst_y >> subsampling_y;
      const int ix4 = x4 >> kWarpedModelPrecisionBits;
      const int iy4 = y4 >> kWarpedModelPrecisionBits;
      // A prediction block may fall outside the frame's boundaries. If a
      // prediction block is calculated using only samples outside the frame's
      // boundary, the filtering can be simplified. We can divide the plane
      // into several regions and handle them differently.
      //
      //                |           |
      //            1   |     3     |   1
      //                |           |
      //         -------+-----------+-------
      //                |***********|
      //            2   |*****4*****|   2
      //                |***********|
      //         -------+-----------+-------
      //                |           |
      //            1   |     3     |   1
      //                |           |
      //
      // At the center, region 4 represents the frame and is the general case.
      //
      // In regions 1 and 2, the prediction block is outside the frame's
      // boundary horizontally. Therefore the horizontal filtering can be
      // simplified. Furthermore, in the region 1 (at the four corners), the
      // prediction is outside the frame's boundary both horizontally and
      // vertically, so we get a constant prediction block.
      //
      // In region 3, the prediction block is outside the frame's boundary
      // vertically. Unfortunately because we apply the horizontal filters
      // first, by the time we apply the vertical filters, they no longer see
      // simple inputs. So the only simplification is that all the rows are
      // the same, but we still need to apply all the horizontal and vertical
      // filters.

      // Check for two simple special cases, where the horizontal filter can
      // be significantly simplified.
      //
      // In general, for each row, the horizontal filter is calculated as
      // follows:
      //   for (int x = -4; x < 4; ++x) {
      //     const int offset = ...;
      //     int sum = horizontal_offset;
      //     for (int k = 0; k < 8; ++k) {
      //       const int column = Clip3(ix4 + x + k - 3, 0, source_width - 1);
      //       sum += kWarpedFilters[offset][k] * src_row[column];
      //     }
      //     ...
      //   }
      // The column index before clipping, ix4 + x + k - 3, varies in the range
      // ix4 - 7 <= ix4 + x + k - 3 <= ix4 + 7. If ix4 - 7 >= source_width - 1
      // or ix4 + 7 <= 0, then all the column indexes are clipped to the same
      // border index (source_width - 1 or 0, respectively). Then for each x,
      // the inner for loop of the horizontal filter is reduced to multiplying
      // the border pixel by the sum of the filter coefficients.
      if (ix4 - 7 >= source_width - 1 || ix4 + 7 <= 0) {
        // Regions 1 and 2.
        // Points to the left or right border of the first row of |src|.
        const uint8_t* first_row_border =
            (ix4 + 7 <= 0) ? src : src + source_width - 1;
        // In general, for y in [-7, 8), the row number iy4 + y is clipped:
        //   const int row = Clip3(iy4 + y, 0, source_height - 1);
        // In two special cases, iy4 + y is clipped to either 0 or
        // source_height - 1 for all y. In the rest of the cases, iy4 + y is
        // bounded and we can avoid clipping iy4 + y by relying on a reference
        // frame's boundary extension on the top and bottom.
        if (iy4 - 7 >= source_height - 1 || iy4 + 7 <= 0) {
          // Region 1.
          // Every sample used to calculate the prediction block has the same
          // value. So the whole prediction block has the same value.
          const int row = (iy4 + 7 <= 0) ? 0 : source_height - 1;
          const uint8_t row_border_pixel =
              first_row_border[row * source_stride];
          int sum = vertical_offset +
                    (horizontal_offset << (7 - kInterRoundBitsHorizontal)) +
                    (row_border_pixel << (14 - kInterRoundBitsHorizontal));
          assert((sum & 2047) == 0);  // 2^11 - 1 = 2047.
          assert(inter_round_bits_vertical <= 11);
          sum >>= inter_round_bits_vertical;
          assert(sum >= 0 && sum <= UINT16_MAX);
          const uint16x8_t sum_vec = vdupq_n_u16(sum);
          uint8x8_t sum_clip;
          if (clip) {
            const uint16x8_t single_round_offset =
                vdupq_n_u16(kSingleRoundOffset);
            const uint16x8_t sum_descale =
                vqsubq_u16(sum_vec, single_round_offset);
            sum_clip = vqmovn_u16(sum_descale);
          }

          DestType* dst_row = dst + start_x - block_start_x;
          for (int y = 0; y < 8; ++y) {
            if (clip) {
              vst1_u8(reinterpret_cast<uint8_t*>(dst_row), sum_clip);
            } else {
              vst1q_u16(reinterpret_cast<uint16_t*>(dst_row), sum_vec);
            }
            dst_row += dest_stride;
          }
          // End of region 1. Continue the |start_x| do-while loop.
          start_x += 8;
          continue;
        }

        // Region 2.
        // Horizontal filter.
        for (int y = -7; y < 8; ++y) {
          // We may over-read up to 13 pixels above the top source row, or up
          // to 13 pixels below the bottom source row. This is proved in
          // warp.cc.
          const int row = iy4 + y;
          const uint8_t row_border_pixel =
              first_row_border[row * source_stride];
          // Every sample is equal to |row_border_pixel|. Since the sum of the
          // warped filter coefficients is 128 (= 2^7), the filtering is
          // equivalent to multiplying |row_border_pixel| by 128.
          intermediate_result_column[y + 7] =
              (horizontal_offset >> kInterRoundBitsHorizontal) +
              (row_border_pixel << (7 - kInterRoundBitsHorizontal));
        }
        // Vertical filter.
        DestType* dst_row = dst + start_x - block_start_x;
        int sy4 =
            (y4 & ((1 << kWarpedModelPrecisionBits) - 1)) - MultiplyBy4(delta);
        for (int y = 0; y < 8; ++y) {
          int sy = sy4 - MultiplyBy4(gamma);
#if defined(__aarch64__)
          const int16x8_t intermediate =
              vld1q_s16(&intermediate_result_column[y]);
          uint16_t tmp[8];
          for (int x = 0; x < 8; ++x) {
            const int offset =
                RightShiftWithRounding(sy, kWarpedDiffPrecisionBits) +
                kWarpedPixelPrecisionShifts;
            const int16x8_t filter = vld1q_s16(kWarpedFilters[offset]);
            const int32x4_t product_low =
                vmull_s16(vget_low_s16(filter), vget_low_s16(intermediate));
            const int32x4_t product_high =
                vmull_s16(vget_high_s16(filter), vget_high_s16(intermediate));
            // vaddvq_s32 is only available on __aarch64__.
            const int32_t sum = vertical_offset + vaddvq_s32(product_low) +
                                vaddvq_s32(product_high);
            const uint16_t sum_descale =
                RightShiftWithRounding(sum, inter_round_bits_vertical);
            if (clip) {
              tmp[x] = sum_descale;
            } else {
              dst_row[x] = sum_descale;
            }
            sy += gamma;
          }
          if (clip) {
            uint16x8_t sum_descale = vld1q_u16(tmp);
            const uint16x8_t single_round_offset =
                vdupq_n_u16(kSingleRoundOffset);
            const uint16x8_t sum_clip =
                vqsubq_u16(sum_descale, single_round_offset);
            vst1_u8(reinterpret_cast<uint8_t*>(dst_row), vqmovn_u16(sum_clip));
          }
#else   // !defined(__aarch64__)
          int16x8_t filter[8];
          for (int x = 0; x < 8; ++x) {
            const int offset =
                RightShiftWithRounding(sy, kWarpedDiffPrecisionBits) +
                kWarpedPixelPrecisionShifts;
            filter[x] = vld1q_s16(kWarpedFilters[offset]);
            sy += gamma;
          }
          Transpose8x8(filter);
          int32x4_t sum_low = vdupq_n_s32(vertical_offset);
          int32x4_t sum_high = sum_low;
          for (int k = 0; k < 8; ++k) {
            const int16_t intermediate = intermediate_result_column[y + k];
            sum_low =
                vmlal_n_s16(sum_low, vget_low_s16(filter[k]), intermediate);
            sum_high =
                vmlal_n_s16(sum_high, vget_high_s16(filter[k]), intermediate);
          }
          assert(inter_round_bits_vertical == 7 ||
                 inter_round_bits_vertical == 11);
          uint16x4_t sum_low_16;
          uint16x4_t sum_high_16;
          if (inter_round_bits_vertical == 7) {
            sum_low_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_low), 7);
            sum_high_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_high), 7);
          } else {
            sum_low_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_low), 11);
            sum_high_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_high), 11);
          }
          if (clip) {
            const uint16x8_t sum = vcombine_u16(sum_low_16, sum_high_16);
            const uint16x8_t single_round_offset =
                vdupq_n_u16(kSingleRoundOffset);
            const uint16x8_t sum_clip = vqsubq_u16(sum, single_round_offset);
            vst1_u8(reinterpret_cast<uint8_t*>(dst_row), vqmovn_u16(sum_clip));
          } else {
            vst1_u16(reinterpret_cast<uint16_t*>(dst_row), sum_low_16);
            vst1_u16(reinterpret_cast<uint16_t*>(dst_row) + 4, sum_high_16);
          }
#endif  // defined(__aarch64__)
          dst_row += dest_stride;
          sy4 += delta;
        }
        // End of region 2. Continue the |start_x| do-while loop.
        start_x += 8;
        continue;
      }

      // Regions 3 and 4.
      // At this point, we know ix4 - 7 < source_width - 1 and ix4 + 7 > 0.

      // In general, for y in [-7, 8), the row number iy4 + y is clipped:
      //   const int row = Clip3(iy4 + y, 0, source_height - 1);
      // In two special cases, iy4 + y is clipped to either 0 or
      // source_height - 1 for all y. In the rest of the cases, iy4 + y is
      // bounded and we can avoid clipping iy4 + y by relying on a reference
      // frame's boundary extension on the top and bottom.
      if (iy4 - 7 >= source_height - 1 || iy4 + 7 <= 0) {
        // Region 3.
        // Horizontal filter.
        const int row = (iy4 + 7 <= 0) ? 0 : source_height - 1;
        const uint8_t* const src_row = src + row * source_stride;
        // Read 15 samples from &src_row[ix4 - 7]. The 16th sample is also
        // read but is ignored.
        //
        // NOTE: This may read up to 13 bytes before src_row[0] or up to 14
        // bytes after src_row[source_width - 1]. We assume the source frame
        // has left and right borders of at least 13 bytes that extend the
        // frame boundary pixels. We also assume there is at least one extra
        // padding byte after the right border of the last source row.
        //
        // TODO(wtc): Convert src_row_u8 to int8 (subtract 128).
        const uint8x16_t src_row_u8 = vld1q_u8(&src_row[ix4 - 7]);
        const int16x8_t src_row_low_s16 =
            vreinterpretq_s16_u16(vmovl_u8(vget_low_u8(src_row_u8)));
        const int16x8_t src_row_high_s16 =
            vreinterpretq_s16_u16(vmovl_u8(vget_high_u8(src_row_u8)));
        int sx4 = (x4 & ((1 << kWarpedModelPrecisionBits) - 1)) - beta * 7;
        for (int y = -7; y < 8; ++y) {
          HorizontalFilter(sx4, alpha, horizontal_offset, src_row_low_s16,
                           src_row_high_s16, intermediate_result[y + 7]);
          sx4 += beta;
        }
      } else {
        // Region 4.
        // Horizontal filter.
        int sx4 = (x4 & ((1 << kWarpedModelPrecisionBits) - 1)) - beta * 7;
        for (int y = -7; y < 8; ++y) {
          // We may over-read up to 13 pixels above the top source row, or up
          // to 13 pixels below the bottom source row. This is proved in
          // warp.cc.
          const int row = iy4 + y;
          const uint8_t* const src_row = src + row * source_stride;
          // Read 15 samples from &src_row[ix4 - 7]. The 16th sample is also
          // read but is ignored.
          //
          // NOTE: This may read up to 13 bytes before src_row[0] or up to 14
          // bytes after src_row[source_width - 1]. We assume the source frame
          // has left and right borders of at least 13 bytes that extend the
          // frame boundary pixels. We also assume there is at least one extra
          // padding byte after the right border of the last source row.
          //
          // TODO(wtc): Convert src_row_u8 to int8 (subtract 128).
          const uint8x16_t src_row_u8 = vld1q_u8(&src_row[ix4 - 7]);
          const int16x8_t src_row_low_s16 =
              vreinterpretq_s16_u16(vmovl_u8(vget_low_u8(src_row_u8)));
          const int16x8_t src_row_high_s16 =
              vreinterpretq_s16_u16(vmovl_u8(vget_high_u8(src_row_u8)));
          HorizontalFilter(sx4, alpha, horizontal_offset, src_row_low_s16,
                           src_row_high_s16, intermediate_result[y + 7]);
          sx4 += beta;
        }
      }

      // Regions 3 and 4.
      // Vertical filter.
      DestType* dst_row = dst + start_x - block_start_x;
      int sy4 =
          (y4 & ((1 << kWarpedModelPrecisionBits) - 1)) - MultiplyBy4(delta);
      for (int y = 0; y < 8; ++y) {
        int sy = sy4 - MultiplyBy4(gamma);
        int16x8_t filter[8];
        for (int x = 0; x < 8; ++x) {
          const int offset =
              RightShiftWithRounding(sy, kWarpedDiffPrecisionBits) +
              kWarpedPixelPrecisionShifts;
          filter[x] = vld1q_s16(kWarpedFilters[offset]);
          sy += gamma;
        }
        Transpose8x8(filter);
        // Similar to horizontal_offset, vertical_offset guarantees sum before
        // shifting is nonnegative.
        //
        // Proof:
        // The range of an entry in intermediate_result is
        //   0 <= intermediate_result[y][x] < 2^13
        // The range of the vertical filter output is
        //   -47 * 2^13 < output < 175 * 2^13
        // Since -2^19 < -47 * 2^13, adding -2^19 (= vertical_offset) to the
        // vertical filter output produces a positive value:
        //   0 < output + 2^19 < 175 * 2^13 + 2^19
        // The final rounding right shift by either 7 or 11 bits adds at most
        // 2^10 to the sum:
        //   0 < output + 2^19 + rounding < 175 * 2^13 + 2^19 + 2^10 = 1958912
        // Since 1958912 = 0x1DE400 < 2^22, shifting it right by 7 or 11 bits
        // brings the value under 2^15, which fits in uint16_t.
        int32x4_t sum_low = vdupq_n_s32(vertical_offset);
        int32x4_t sum_high = sum_low;
        for (int k = 0; k < 8; ++k) {
          const int16x8_t intermediate = vld1q_s16(intermediate_result[y + k]);
          sum_low = vmlal_s16(sum_low, vget_low_s16(filter[k]),
                              vget_low_s16(intermediate));
          sum_high = vmlal_s16(sum_high, vget_high_s16(filter[k]),
                               vget_high_s16(intermediate));
        }
        assert(inter_round_bits_vertical == 7 ||
               inter_round_bits_vertical == 11);
        // Since inter_round_bits_vertical can be 7 or 11, and all the narrowing
        // shift intrinsics require the shift argument to be a constant
        // (immediate), we have two options:
        // 1. Call a non-narrowing shift, followed by a narrowing extract.
        // 2. Call a narrowing shift (with a constant shift of 7 or 11) in an
        //    if-else statement.
#if defined(__aarch64__)
        // This version is slightly faster for arm64 (1106 ms vs 1112 ms).
        // This version is slower for 32-bit arm (1235 ms vs 1149 ms).
        const int32x4_t shift = vdupq_n_s32(-inter_round_bits_vertical);
        const uint32x4_t sum_low_shifted =
            vrshlq_u32(vreinterpretq_u32_s32(sum_low), shift);
        const uint32x4_t sum_high_shifted =
            vrshlq_u32(vreinterpretq_u32_s32(sum_high), shift);
        const uint16x4_t sum_low_16 = vmovn_u32(sum_low_shifted);
        const uint16x4_t sum_high_16 = vmovn_u32(sum_high_shifted);
#else   // !defined(__aarch64__)
        // This version is faster for 32-bit arm.
        // This version is slightly slower for arm64.
        uint16x4_t sum_low_16;
        uint16x4_t sum_high_16;
        if (inter_round_bits_vertical == 7) {
          sum_low_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_low), 7);
          sum_high_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_high), 7);
        } else {
          sum_low_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_low), 11);
          sum_high_16 = vrshrn_n_u32(vreinterpretq_u32_s32(sum_high), 11);
        }
#endif  // defined(__aarch64__)
        if (clip) {
          const uint16x8_t sum = vcombine_u16(sum_low_16, sum_high_16);
          const uint16x8_t single_round_offset =
              vdupq_n_u16(kSingleRoundOffset);
          const uint16x8_t sum_clip = vqsubq_u16(sum, single_round_offset);
          vst1_u8(reinterpret_cast<uint8_t*>(dst_row), vqmovn_u16(sum_clip));
        } else {
          // vst1q_u16 can also be used:
          //   vst1q_u16(dst_row, vcombine_u16(sum_low_16, sum_high_16));
          // But it is slightly slower for arm64 (the same speed for 32-bit
          // arm).
          //
          // vst1_u16_x2 could be used, but it is also slightly slower for arm64
          // and causes a bus error for 32-bit arm. Also, it is not supported by
          // gcc 7.2.0.
          vst1_u16(reinterpret_cast<uint16_t*>(dst_row), sum_low_16);
          vst1_u16(reinterpret_cast<uint16_t*>(dst_row) + 4, sum_high_16);
        }
        dst_row += dest_stride;
        sy4 += delta;
      }
      start_x += 8;
    } while (start_x < block_start_x + block_width);
    dst += 8 * dest_stride;
    start_y += 8;
  } while (start_y < block_start_y + block_height);
}

void Init8bpp() {
  Dsp* const dsp = dsp_internal::GetWritableDspTable(8);
  assert(dsp != nullptr);
  dsp->warp = Warp_NEON<false>;
  dsp->warp_clip = Warp_NEON<true>;
}

}  // namespace
}  // namespace low_bitdepth

void WarpInit_NEON() { low_bitdepth::Init8bpp(); }

}  // namespace dsp
}  // namespace libgav1
#else   // !LIBGAV1_ENABLE_NEON
namespace libgav1 {
namespace dsp {

void WarpInit_NEON() {}

}  // namespace dsp
}  // namespace libgav1
#endif  // LIBGAV1_ENABLE_NEON
