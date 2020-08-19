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

#include "src/post_filter.h"

#include <algorithm>
#include <atomic>
#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <memory>

#include "src/dsp/constants.h"
#include "src/utils/array_2d.h"
#include "src/utils/blocking_counter.h"
#include "src/utils/compiler_attributes.h"
#include "src/utils/constants.h"
#include "src/utils/logging.h"
#include "src/utils/memory.h"
#include "src/utils/types.h"

namespace libgav1 {
namespace {

constexpr uint8_t kCdefUvDirection[2][2][8] = {
    {{0, 1, 2, 3, 4, 5, 6, 7}, {1, 2, 2, 2, 3, 4, 6, 0}},
    {{7, 0, 2, 4, 5, 6, 6, 6}, {0, 1, 2, 3, 4, 5, 6, 7}}};

// Row indices of deblocked pixels needed by loop restoration. This is used to
// populate the |deblock_buffer_| when cdef is on. The first dimension is
// subsampling_y.
constexpr int kDeblockedRowsForLoopRestoration[2][4] = {{54, 55, 56, 57},
                                                        {26, 27, 28, 29}};

constexpr int SubsampledValue(int value, int subsampling) {
  return (subsampling == 0) ? value : DivideBy2(value + 1);
}

// The following example illustrates how ExtendFrame() extends a frame.
// Suppose the frame width is 8 and height is 4, and left, right, top, and
// bottom are all equal to 3.
//
// Before:
//
//       ABCDEFGH
//       IJKLMNOP
//       QRSTUVWX
//       YZabcdef
//
// After:
//
//   AAA|ABCDEFGH|HHH  [3]
//   AAA|ABCDEFGH|HHH
//   AAA|ABCDEFGH|HHH
//   ---+--------+---
//   AAA|ABCDEFGH|HHH  [1]
//   III|IJKLMNOP|PPP
//   QQQ|QRSTUVWX|XXX
//   YYY|YZabcdef|fff
//   ---+--------+---
//   YYY|YZabcdef|fff  [2]
//   YYY|YZabcdef|fff
//   YYY|YZabcdef|fff
//
// ExtendFrame() first extends the rows to the left and to the right[1]. Then
// it copies the extended last row to the bottom borders[2]. Finally it copies
// the extended first row to the top borders[3].
template <typename Pixel>
void ExtendFrame(uint8_t* const frame_start, const int width, const int height,
                 ptrdiff_t stride, const int left, const int right,
                 const int top, const int bottom) {
  auto* const start = reinterpret_cast<Pixel*>(frame_start);
  const Pixel* src = start;
  Pixel* dst = start - left;
  stride /= sizeof(Pixel);
  // Copy to left and right borders.
  for (int y = 0; y < height; ++y) {
    Memset(dst, src[0], left);
    Memset(dst + (left + width), src[width - 1], right);
    src += stride;
    dst += stride;
  }
  // Copy to bottom borders. For performance we copy |stride| pixels
  // (including some padding pixels potentially) in each row, ending at the
  // bottom right border pixel. In the diagram the asterisks indicate padding
  // pixels.
  //
  // |<--- stride --->|
  // **YYY|YZabcdef|fff <-- Copy from the extended last row.
  // -----+--------+---
  // **YYY|YZabcdef|fff
  // **YYY|YZabcdef|fff
  // **YYY|YZabcdef|fff <-- bottom right border pixel
  assert(src == start + height * stride);
  dst = const_cast<Pixel*>(src) + width + right - stride;
  src = dst - stride;
  for (int y = 0; y < bottom; ++y) {
    memcpy(dst, src, sizeof(Pixel) * stride);
    dst += stride;
  }
  // Copy to top borders. For performance we copy |stride| pixels (including
  // some padding pixels potentially) in each row, starting from the top left
  // border pixel. In the diagram the asterisks indicate padding pixels.
  //
  // +-- top left border pixel
  // |
  // v
  // AAA|ABCDEFGH|HHH**
  // AAA|ABCDEFGH|HHH**
  // AAA|ABCDEFGH|HHH**
  // ---+--------+-----
  // AAA|ABCDEFGH|HHH** <-- Copy from the extended first row.
  // |<--- stride --->|
  src = start - left;
  dst = start - left - top * stride;
  for (int y = 0; y < top; ++y) {
    memcpy(dst, src, sizeof(Pixel) * stride);
    dst += stride;
  }
}

template <typename Pixel>
void ExtendLine(uint8_t* const line_start, const int width, const int left,
                const int right) {
  auto* const start = reinterpret_cast<Pixel*>(line_start);
  const Pixel* src = start;
  Pixel* dst = start - left;
  // Copy to left and right borders.
  Memset(dst, src[0], left);
  Memset(dst + (left + width), src[width - 1], right);
}

template <typename Pixel>
void CopyPlane(const uint8_t* source, int source_stride, const int width,
               const int height, uint8_t* dest, int dest_stride) {
  auto* dst = reinterpret_cast<Pixel*>(dest);
  const auto* src = reinterpret_cast<const Pixel*>(source);
  source_stride /= sizeof(Pixel);
  dest_stride /= sizeof(Pixel);
  for (int y = 0; y < height; ++y) {
    memcpy(dst, src, width * sizeof(Pixel));
    src += source_stride;
    dst += dest_stride;
  }
}

template <int bitdepth, typename Pixel>
void ComputeSuperRes(const uint8_t* source, const int upscaled_width,
                     const int initial_subpixel_x, const int step,
                     uint8_t* const dest) {
  const auto* src = reinterpret_cast<const Pixel*>(source);
  auto* dst = reinterpret_cast<Pixel*>(dest);
  src -= DivideBy2(kSuperResFilterTaps);
  int subpixel_x = initial_subpixel_x;
  for (int x = 0; x < upscaled_width; ++x) {
    int sum = 0;
    const Pixel* const src_x = &src[subpixel_x >> kSuperResScaleBits];
    const int src_x_subpixel =
        (subpixel_x & kSuperResScaleMask) >> kSuperResExtraBits;
    for (int i = 0; i < kSuperResFilterTaps; ++i) {
      sum += src_x[i] * kUpscaleFilter[src_x_subpixel][i];
    }
    dst[x] =
        Clip3(RightShiftWithRounding(sum, kFilterBits), 0, (1 << bitdepth) - 1);
    subpixel_x += step;
  }
}

}  // namespace

// Static data member definitions.
constexpr int PostFilter::kCdefLargeValue;

void PostFilter::ApplyFilteringForOneSuperBlockRow(int row4x4, int sb4x4,
                                                   bool is_last_row) {
  if (row4x4 < 0) return;
  if (DoDeblock()) {
    ApplyDeblockFilterForOneSuperBlockRow(row4x4, sb4x4);
  }
  // Cdef and subsequent filters lag by 1 superblock row relative to deblocking
  // (since deblocking the current superblock row could change the pixels in the
  // previous superblock row).
  const int previous_row4x4 = row4x4 - sb4x4;
  if (previous_row4x4 >= 0) {
    if (DoRestoration() && DoCdef()) {
      SetupDeblockBuffer(previous_row4x4, sb4x4);
    }
    if (DoCdef()) {
      ApplyCdefForOneSuperBlockRow(previous_row4x4, sb4x4);
    }
    if (DoSuperRes()) {
      ApplySuperResForOneSuperBlockRow(previous_row4x4, sb4x4);
    }
    if (DoRestoration()) {
      CopyBorderForRestoration(previous_row4x4, sb4x4);
      ApplyLoopRestorationForOneSuperBlockRow(previous_row4x4, sb4x4);
    }
  }
  if (is_last_row) {
    if (DoRestoration() && DoCdef()) {
      SetupDeblockBuffer(row4x4, sb4x4);
    }
    if (DoCdef()) {
      ApplyCdefForOneSuperBlockRow(row4x4, sb4x4);
    }
    if (DoSuperRes()) {
      ApplySuperResForOneSuperBlockRow(row4x4, sb4x4);
    }
    if (DoRestoration()) {
      CopyBorderForRestoration(row4x4, sb4x4);
      ApplyLoopRestorationForOneSuperBlockRow(row4x4, sb4x4);
      // Loop restoration operates with a lag of 8 rows. So make sure to cover
      // all the rows of the last superblock row.
      ApplyLoopRestorationForOneSuperBlockRow(row4x4 + sb4x4, 16);
    }
    if (DoCdef() || DoRestoration()) {
      // Adjust the frame buffer pointers after cdef/loop restoration was
      // applied. For Cdef and Loop Restoration, we write the filtered output to
      // the upper-left side of the original source_buffer_->data(). The new
      // buffer pointer is still guaranteed to be within the original buffer's
      // data region because of the borders. Negative shifts are used, to
      // indicate shifting towards the upper-left corner. Shifts are in pixels.
      for (int plane = kPlaneY; plane < planes_; ++plane) {
        const bool shift_cdef = DoCdef();
        const bool shift_loop_restoration =
            DoRestoration() &&
            loop_restoration_.type[plane] != kLoopRestorationTypeNone;
        const int horizontal_shift = -source_buffer_->alignment() *
                                     (static_cast<int>(shift_cdef) +
                                      static_cast<int>(shift_loop_restoration));
        const int vertical_shift =
            (shift_cdef ? -kCdefBorder : 0) +
            (shift_loop_restoration ? -kRestorationBorder : 0);
        source_buffer_->ShiftBuffer(plane, horizontal_shift, vertical_shift);
      }
    }
    ExtendBordersForReferenceFrame();
  }
}

bool PostFilter::ApplyFilteringThreaded() {
  if (DoDeblock() && !ApplyDeblockFilterThreaded()) return false;
  if (DoCdef() && DoRestoration()) {
    for (int row4x4 = 0; row4x4 < frame_header_.rows4x4;
         row4x4 += kNum4x4InLoopFilterMaskUnit) {
      SetupDeblockBuffer(row4x4, kNum4x4InLoopFilterMaskUnit);
    }
  }
  if (DoCdef()) ApplyCdef();
  if (DoSuperRes()) {
    ApplySuperRes(source_buffer_, frame_header_.rows4x4, subsampling_y_,
                  /*plane_offsets=*/{0, 0, 0});
  }
  if (DoRestoration()) ApplyLoopRestoration();
  ExtendBordersForReferenceFrame();
  return true;
}

void PostFilter::ExtendBordersForReferenceFrame() {
  if (frame_header_.refresh_frame_flags == 0) return;
  for (int plane = kPlaneY; plane < planes_; ++plane) {
    const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
    const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
    const int plane_width =
        RightShiftWithRounding(upscaled_width_, subsampling_x);
    const int plane_height = RightShiftWithRounding(height_, subsampling_y);
    assert(source_buffer_->left_border(plane) >= kMinLeftBorderPixels &&
           source_buffer_->right_border(plane) >= kMinRightBorderPixels &&
           source_buffer_->top_border(plane) >= kMinTopBorderPixels &&
           source_buffer_->bottom_border(plane) >= kMinBottomBorderPixels);
    // plane subsampling_x_ left_border
    //   Y        N/A         64, 48
    //  U,V        0          64, 48
    //  U,V        1          32, 16
    assert(source_buffer_->left_border(plane) >= 16);
    // The |left| argument to ExtendFrameBoundary() must be at least
    // kMinLeftBorderPixels (13) for warp.
    static_assert(16 >= kMinLeftBorderPixels, "");
    ExtendFrameBoundary(
        source_buffer_->data(plane), plane_width, plane_height,
        source_buffer_->stride(plane), source_buffer_->left_border(plane),
        source_buffer_->right_border(plane), source_buffer_->top_border(plane),
        source_buffer_->bottom_border(plane));
  }
}

bool PostFilter::DoRestoration() const {
  return DoRestoration(loop_restoration_, do_post_filter_mask_, planes_);
}

bool PostFilter::DoRestoration(const LoopRestoration& loop_restoration,
                               uint8_t do_post_filter_mask, int num_planes) {
  if ((do_post_filter_mask & 0x08) == 0) return false;
  if (num_planes == kMaxPlanesMonochrome) {
    return loop_restoration.type[kPlaneY] != kLoopRestorationTypeNone;
  }
  return loop_restoration.type[kPlaneY] != kLoopRestorationTypeNone ||
         loop_restoration.type[kPlaneU] != kLoopRestorationTypeNone ||
         loop_restoration.type[kPlaneV] != kLoopRestorationTypeNone;
}

void PostFilter::ExtendFrameBoundary(uint8_t* const frame_start,
                                     const int width, const int height,
                                     const ptrdiff_t stride, const int left,
                                     const int right, const int top,
                                     const int bottom) {
#if LIBGAV1_MAX_BITDEPTH >= 10
  if (bitdepth_ >= 10) {
    ExtendFrame<uint16_t>(frame_start, width, height, stride, left, right, top,
                          bottom);
    return;
  }
#endif
  ExtendFrame<uint8_t>(frame_start, width, height, stride, left, right, top,
                       bottom);
}

void PostFilter::DeblockFilterWorker(const DeblockFilterJob* jobs, int num_jobs,
                                     std::atomic<int>* job_counter,
                                     DeblockFilter deblock_filter) {
  int job_index;
  while ((job_index = job_counter->fetch_add(1, std::memory_order_relaxed)) <
         num_jobs) {
    const DeblockFilterJob& job = jobs[job_index];
    for (int column4x4 = 0, column_unit = 0;
         column4x4 < frame_header_.columns4x4;
         column4x4 += kNum4x4InLoopFilterMaskUnit, ++column_unit) {
      const int unit_id = GetDeblockUnitId(job.row_unit, column_unit);
      (this->*deblock_filter)(static_cast<Plane>(job.plane), job.row4x4,
                              column4x4, unit_id);
    }
  }
}

bool PostFilter::ApplyDeblockFilterThreaded() {
  const int jobs_per_plane = DivideBy16(frame_header_.rows4x4 + 15);
  const int num_workers = thread_pool_->num_threads();
  int planes[kMaxPlanes];
  planes[0] = kPlaneY;
  int num_planes = 1;
  for (int plane = kPlaneU; plane < planes_; ++plane) {
    if (frame_header_.loop_filter.level[plane + 1] != 0) {
      planes[num_planes++] = plane;
    }
  }
  const int num_jobs = num_planes * jobs_per_plane;
  std::unique_ptr<DeblockFilterJob[]> jobs_unique_ptr(
      new (std::nothrow) DeblockFilterJob[num_jobs]);
  if (jobs_unique_ptr == nullptr) return false;
  DeblockFilterJob* jobs = jobs_unique_ptr.get();
  // The vertical filters are not dependent on each other. So simply schedule
  // them for all possible rows.
  //
  // The horizontal filter for a row/column depends on the vertical filter being
  // finished for the blocks to the top and to the right. To work around
  // this synchronization, we simply wait for the vertical filter to finish for
  // all rows. Now, the horizontal filters can also be scheduled
  // unconditionally similar to the vertical filters.
  //
  // The only synchronization involved is to know when the each directional
  // filter is complete for the entire frame.
  for (auto& type : {kLoopFilterTypeVertical, kLoopFilterTypeHorizontal}) {
    const DeblockFilter deblock_filter =
        deblock_filter_type_table_[kDeblockFilterBitMask][type];
    int job_index = 0;
    for (int i = 0; i < num_planes; ++i) {
      const int plane = planes[i];
      for (int row4x4 = 0, row_unit = 0; row4x4 < frame_header_.rows4x4;
           row4x4 += kNum4x4InLoopFilterMaskUnit, ++row_unit) {
        assert(job_index < num_jobs);
        DeblockFilterJob& job = jobs[job_index++];
        job.plane = plane;
        job.row4x4 = row4x4;
        job.row_unit = row_unit;
      }
    }
    assert(job_index == num_jobs);
    std::atomic<int> job_counter(0);
    BlockingCounter pending_workers(num_workers);
    for (int i = 0; i < num_workers; ++i) {
      thread_pool_->Schedule([this, jobs, num_jobs, &job_counter,
                              deblock_filter, &pending_workers]() {
        DeblockFilterWorker(jobs, num_jobs, &job_counter, deblock_filter);
        pending_workers.Decrement();
      });
    }
    // Run the jobs on the current thread.
    DeblockFilterWorker(jobs, num_jobs, &job_counter, deblock_filter);
    // Wait for the threadpool jobs to finish.
    pending_workers.Wait();
  }
  return true;
}

void PostFilter::SetupDeblockBuffer(int row4x4_start, int sb4x4) {
  assert(row4x4_start >= 0);
  assert(DoCdef());
  assert(DoRestoration());
  for (int sb_y = 0; sb_y < sb4x4; sb_y += 16) {
    const int row4x4 = row4x4_start + sb_y;
    for (int plane = 0; plane < planes_; ++plane) {
      CopyDeblockedPixels(static_cast<Plane>(plane), row4x4);
    }
    const int row_unit = DivideBy16(row4x4);
    const int row_offset_start = MultiplyBy4(row_unit + 1);
    if (DoSuperRes()) {
      std::array<ptrdiff_t, kMaxPlanes> plane_offsets{
          row_offset_start * deblock_buffer_.stride(kPlaneY),
          row_offset_start * deblock_buffer_.stride(kPlaneU),
          row_offset_start * deblock_buffer_.stride(kPlaneV)};
      ApplySuperRes(&deblock_buffer_, /*rows4x4=*/1, /*chroma_subsampling_y=*/0,
                    plane_offsets);
    }
    // Extend the left and right boundaries needed for loop restoration.
    for (int plane = 0; plane < planes_; ++plane) {
      uint8_t* src = deblock_buffer_.data(plane) +
                     row_offset_start * deblock_buffer_.stride(plane);
      const int subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
      const int plane_width =
          RightShiftWithRounding(upscaled_width_, subsampling_x);
      for (int i = 0; i < 4; ++i) {
#if LIBGAV1_MAX_BITDEPTH >= 10
        if (bitdepth_ >= 10) {
          ExtendLine<uint16_t>(src, plane_width, kRestorationBorder,
                               kRestorationBorder);
        } else  // NOLINT.
#endif
        {
          ExtendLine<uint8_t>(src, plane_width, kRestorationBorder,
                              kRestorationBorder);
        }
        src += deblock_buffer_.stride(plane);
      }
    }
  }
}

void PostFilter::CopyDeblockedPixels(Plane plane, int row4x4) {
  const int subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
  const int subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
  const ptrdiff_t src_stride = source_buffer_->stride(plane);
  const uint8_t* const src =
      SetBufferOffset(source_buffer_, static_cast<Plane>(plane), row4x4, 0,
                      subsampling_x, subsampling_y);
  const ptrdiff_t dst_stride = deblock_buffer_.stride(plane);
  const int row_unit = DivideBy16(row4x4);
  // First 4 rows of |deblock_buffer_| are never populated since they will not
  // be used by loop restoration. So |row_unit| is offset by 1.
  const int row_offset = MultiplyBy4(row_unit + 1);
  uint8_t* dst = deblock_buffer_.data(plane) + dst_stride * row_offset;
  const int num_pixels =
      SubsampledValue(MultiplyBy4(frame_header_.columns4x4), subsampling_x);
  int last_valid_row = -1;
  const int plane_height = SubsampledValue(frame_header_.height, subsampling_y);
  for (int i = 0; i < 4; ++i) {
    int row = kDeblockedRowsForLoopRestoration[subsampling_y][i];
    const int absolute_row = (MultiplyBy4(row4x4) >> subsampling_y) + row;
    if (absolute_row >= plane_height) {
      if (last_valid_row == -1) {
        // We have run out of rows and there no valid row to copy. This will not
        // be used by loop restoration, so we can simply break here. However,
        // MSAN does not know that this is never used (since we sometimes apply
        // superres to this row as well). So zero it out in case of MSAN.
#if LIBGAV1_MSAN
        if (DoSuperRes()) {
          memset(dst, 0, num_pixels * pixel_size_);
          dst += dst_stride;
          continue;
        }
#endif
        break;
      }
      // If we run out of rows, copy the last valid row (mimics the bottom
      // border extension).
      row = last_valid_row;
    }
    memcpy(dst, src + src_stride * row, num_pixels * pixel_size_);
    last_valid_row = row;
    dst += dst_stride;
  }
}

void PostFilter::ApplyDeblockFilterForOneSuperBlockRow(int row4x4_start,
                                                       int sb4x4) {
  assert(row4x4_start >= 0);
  assert(DoDeblock());
  for (int plane = kPlaneY; plane < planes_; ++plane) {
    if (plane != kPlaneY && frame_header_.loop_filter.level[plane + 1] == 0) {
      continue;
    }

    for (int y = 0; y < sb4x4; y += 16) {
      const int row4x4 = row4x4_start + y;
      if (row4x4 >= frame_header_.rows4x4) break;
      int column4x4;
      for (column4x4 = 0; column4x4 < frame_header_.columns4x4;
           column4x4 += kNum4x4InLoopFilterMaskUnit) {
        // First apply vertical filtering
        VerticalDeblockFilterNoMask(static_cast<Plane>(plane), row4x4,
                                    column4x4, 0);

        // Delay one superblock to apply horizontal filtering.
        if (column4x4 != 0) {
          HorizontalDeblockFilterNoMask(static_cast<Plane>(plane), row4x4,
                                        column4x4 - kNum4x4InLoopFilterMaskUnit,
                                        0);
        }
      }
      // Horizontal filtering for the last 64x64 block.
      HorizontalDeblockFilterNoMask(static_cast<Plane>(plane), row4x4,
                                    column4x4 - kNum4x4InLoopFilterMaskUnit, 0);
    }
  }
}

void PostFilter::ComputeDeblockFilterLevels(
    const int8_t delta_lf[kFrameLfCount],
    uint8_t deblock_filter_levels[kMaxSegments][kFrameLfCount]
                                 [kNumReferenceFrameTypes][2]) const {
  if (!DoDeblock()) return;
  for (int segment_id = 0;
       segment_id < (frame_header_.segmentation.enabled ? kMaxSegments : 1);
       ++segment_id) {
    int level_index = 0;
    for (; level_index < 2; ++level_index) {
      LoopFilterMask::ComputeDeblockFilterLevels(
          frame_header_, segment_id, level_index, delta_lf,
          deblock_filter_levels[segment_id][level_index]);
    }
    for (; level_index < kFrameLfCount; ++level_index) {
      if (frame_header_.loop_filter.level[level_index] != 0) {
        LoopFilterMask::ComputeDeblockFilterLevels(
            frame_header_, segment_id, level_index, delta_lf,
            deblock_filter_levels[segment_id][level_index]);
      }
    }
  }
}

uint8_t* PostFilter::GetCdefBufferAndStride(
    const int start_x, const int start_y, const int plane,
    const int subsampling_x, const int subsampling_y,
    const int window_buffer_plane_size, const int vertical_shift,
    const int horizontal_shift, int* cdef_stride) const {
  if (thread_pool_ != nullptr) {
    // write output to threaded_window_buffer.
    *cdef_stride = window_buffer_width_ * pixel_size_;
    const int column_window = start_x % (window_buffer_width_ >> subsampling_x);
    const int row_window = start_y % (window_buffer_height_ >> subsampling_y);
    return threaded_window_buffer_ + plane * window_buffer_plane_size +
           row_window * (*cdef_stride) + column_window * pixel_size_;
  }
  // write output to source_buffer_.
  *cdef_stride = source_buffer_->stride(plane);
  // In-place cdef is applied by writing the output to the top-left corner.
  const ptrdiff_t buffer_offset =
      vertical_shift * (*cdef_stride) + horizontal_shift * pixel_size_;
  return source_buffer_->data(plane) + start_y * (*cdef_stride) +
         start_x * pixel_size_ + buffer_offset;
}

template <typename Pixel>
void PostFilter::ApplyCdefForOneUnit(uint16_t* cdef_block, const int index,
                                     const int block_width4x4,
                                     const int block_height4x4,
                                     const int row4x4_start,
                                     const int column4x4_start) {
  const int coeff_shift = bitdepth_ - 8;
  const int step = kNum4x4BlocksWide[kBlock8x8];
  // abs(horizontal_shift) must be at least kCdefBorder. Increase it to
  // preserve buffer alignment.
  const int horizontal_shift = -source_buffer_->alignment();
  const int vertical_shift = -kCdefBorder;
  const int window_buffer_plane_size =
      window_buffer_width_ * window_buffer_height_ * pixel_size_;

  if (index == -1) {
    for (int plane = kPlaneY; plane < planes_; ++plane) {
      const int subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
      const int subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
      const int start_x = MultiplyBy4(column4x4_start) >> subsampling_x;
      const int start_y = MultiplyBy4(row4x4_start) >> subsampling_y;
      int cdef_stride;
      uint8_t* const cdef_buffer = GetCdefBufferAndStride(
          start_x, start_y, plane, subsampling_x, subsampling_y,
          window_buffer_plane_size, vertical_shift, horizontal_shift,
          &cdef_stride);
      const int src_stride = source_buffer_->stride(plane);
      uint8_t* const src_buffer = source_buffer_->data(plane) +
                                  start_y * src_stride + start_x * pixel_size_;
      const int block_width = MultiplyBy4(block_width4x4) >> subsampling_x;
      const int block_height = MultiplyBy4(block_height4x4) >> subsampling_y;
      for (int y = 0; y < block_height; ++y) {
        memcpy(cdef_buffer + y * cdef_stride, src_buffer + y * src_stride,
               block_width * pixel_size_);
      }
    }
    return;
  }

  PrepareCdefBlock<Pixel>(source_buffer_, planes_, subsampling_x_,
                          subsampling_y_, frame_header_.width,
                          frame_header_.height, block_width4x4, block_height4x4,
                          row4x4_start, column4x4_start, cdef_block,
                          kRestorationProcessingUnitSizeWithBorders);

  for (int row4x4 = row4x4_start; row4x4 < row4x4_start + block_height4x4;
       row4x4 += step) {
    for (int column4x4 = column4x4_start;
         column4x4 < column4x4_start + block_width4x4; column4x4 += step) {
      const bool skip =
          block_parameters_.Find(row4x4, column4x4) != nullptr &&
          block_parameters_.Find(row4x4 + 1, column4x4) != nullptr &&
          block_parameters_.Find(row4x4, column4x4 + 1) != nullptr &&
          block_parameters_.Find(row4x4 + 1, column4x4 + 1) != nullptr &&
          block_parameters_.Find(row4x4, column4x4)->skip &&
          block_parameters_.Find(row4x4 + 1, column4x4)->skip &&
          block_parameters_.Find(row4x4, column4x4 + 1)->skip &&
          block_parameters_.Find(row4x4 + 1, column4x4 + 1)->skip;
      int damping = frame_header_.cdef.damping + coeff_shift;
      int direction_y;
      int direction;
      int variance;
      uint8_t primary_strength;
      uint8_t secondary_strength;

      for (int plane = kPlaneY; plane < planes_; ++plane) {
        const int subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
        const int subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
        const int start_x = MultiplyBy4(column4x4) >> subsampling_x;
        const int start_y = MultiplyBy4(row4x4) >> subsampling_y;
        const int block_width = 8 >> subsampling_x;
        const int block_height = 8 >> subsampling_y;
        int cdef_stride;
        uint8_t* const cdef_buffer = GetCdefBufferAndStride(
            start_x, start_y, plane, subsampling_x, subsampling_y,
            window_buffer_plane_size, vertical_shift, horizontal_shift,
            &cdef_stride);
        const int src_stride = source_buffer_->stride(plane);
        uint8_t* const src_buffer = source_buffer_->data(plane) +
                                    start_y * src_stride +
                                    start_x * pixel_size_;

        if (skip) {  // No cdef filtering.
          for (int y = 0; y < block_height; ++y) {
            memcpy(cdef_buffer + y * cdef_stride, src_buffer + y * src_stride,
                   block_width * pixel_size_);
          }
          continue;
        }

        if (plane == kPlaneY) {
          dsp_.cdef_direction(src_buffer, src_stride, &direction_y, &variance);
          primary_strength = frame_header_.cdef.y_primary_strength[index]
                             << coeff_shift;
          secondary_strength = frame_header_.cdef.y_secondary_strength[index]
                               << coeff_shift;
          direction = (primary_strength == 0) ? 0 : direction_y;
          const int variance_strength =
              ((variance >> 6) != 0) ? std::min(FloorLog2(variance >> 6), 12)
                                     : 0;
          primary_strength =
              (variance != 0)
                  ? (primary_strength * (4 + variance_strength) + 8) >> 4
                  : 0;
        } else {
          primary_strength = frame_header_.cdef.uv_primary_strength[index]
                             << coeff_shift;
          secondary_strength = frame_header_.cdef.uv_secondary_strength[index]
                               << coeff_shift;
          direction = (primary_strength == 0)
                          ? 0
                          : kCdefUvDirection[subsampling_x_][subsampling_y_]
                                            [direction_y];
          damping = frame_header_.cdef.damping + coeff_shift - 1;
        }

        if ((primary_strength | secondary_strength) == 0) {
          for (int y = 0; y < block_height; ++y) {
            memcpy(cdef_buffer + y * cdef_stride, src_buffer + y * src_stride,
                   block_width * pixel_size_);
          }
          continue;
        }
        uint16_t* cdef_src =
            cdef_block + plane * kRestorationProcessingUnitSizeWithBorders *
                             kRestorationProcessingUnitSizeWithBorders;
        cdef_src += kCdefBorder * kRestorationProcessingUnitSizeWithBorders +
                    kCdefBorder;
        cdef_src += (MultiplyBy4(row4x4 - row4x4_start) >> subsampling_y) *
                        kRestorationProcessingUnitSizeWithBorders +
                    (MultiplyBy4(column4x4 - column4x4_start) >> subsampling_x);
        dsp_.cdef_filter(cdef_src, kRestorationProcessingUnitSizeWithBorders,
                         frame_header_.rows4x4, frame_header_.columns4x4,
                         start_x, start_y, subsampling_x, subsampling_y,
                         primary_strength, secondary_strength, damping,
                         direction, cdef_buffer, cdef_stride);
      }
    }
  }
}

template <typename Pixel>
void PostFilter::ApplyCdefForOneRowInWindow(const int row4x4,
                                            const int column4x4_start) {
  const int step_64x64 = 16;  // = 64/4.
  uint16_t cdef_block[kRestorationProcessingUnitSizeWithBorders *
                      kRestorationProcessingUnitSizeWithBorders * 3];

  for (int column4x4_64x64 = 0;
       column4x4_64x64 < std::min(DivideBy4(window_buffer_width_),
                                  frame_header_.columns4x4 - column4x4_start);
       column4x4_64x64 += step_64x64) {
    const int column4x4 = column4x4_start + column4x4_64x64;
    const int index = cdef_index_[DivideBy16(row4x4)][DivideBy16(column4x4)];
    const int block_width4x4 =
        std::min(step_64x64, frame_header_.columns4x4 - column4x4);
    const int block_height4x4 =
        std::min(step_64x64, frame_header_.rows4x4 - row4x4);

    ApplyCdefForOneUnit<Pixel>(cdef_block, index, block_width4x4,
                               block_height4x4, row4x4, column4x4);
  }
}

// Each thread processes one row inside the window.
// Y, U, V planes are processed together inside one thread.
template <typename Pixel>
void PostFilter::ApplyCdefThreaded() {
  assert((window_buffer_height_ & 63) == 0);
  const int num_workers = thread_pool_->num_threads();
  // abs(horizontal_shift) must be at least kCdefBorder. Increase it to
  // preserve buffer alignment.
  const int horizontal_shift = -source_buffer_->alignment();
  const int vertical_shift = -kCdefBorder;
  const int window_buffer_plane_size =
      window_buffer_width_ * window_buffer_height_ * pixel_size_;
  const int window_buffer_height4x4 = DivideBy4(window_buffer_height_);
  const int step_64x64 = 16;  // = 64/4.
  for (int row4x4 = 0; row4x4 < frame_header_.rows4x4;
       row4x4 += window_buffer_height4x4) {
    const int actual_window_height4x4 =
        std::min(window_buffer_height4x4, frame_header_.rows4x4 - row4x4);
    const int vertical_units_per_window =
        DivideBy16(actual_window_height4x4 + 15);
    for (int column4x4 = 0; column4x4 < frame_header_.columns4x4;
         column4x4 += DivideBy4(window_buffer_width_)) {
      const int jobs_for_threadpool =
          vertical_units_per_window * num_workers / (num_workers + 1);
      BlockingCounter pending_jobs(jobs_for_threadpool);
      int job_count = 0;
      for (int row64x64 = 0; row64x64 < actual_window_height4x4;
           row64x64 += step_64x64) {
        if (job_count < jobs_for_threadpool) {
          thread_pool_->Schedule(
              [this, row4x4, column4x4, row64x64, &pending_jobs]() {
                ApplyCdefForOneRowInWindow<Pixel>(row4x4 + row64x64, column4x4);
                pending_jobs.Decrement();
              });
        } else {
          ApplyCdefForOneRowInWindow<Pixel>(row4x4 + row64x64, column4x4);
        }
        ++job_count;
      }
      pending_jobs.Wait();

      // Copy |threaded_window_buffer_| to |source_buffer_|.
      for (int plane = kPlaneY; plane < planes_; ++plane) {
        const int src_stride = source_buffer_->stride(plane);
        const ptrdiff_t buffer_offset =
            vertical_shift * src_stride + horizontal_shift * pixel_size_;
        const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
        const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
        const int plane_row = MultiplyBy4(row4x4) >> subsampling_y;
        const int plane_column = MultiplyBy4(column4x4) >> subsampling_x;
        int copy_width = std::min(frame_header_.columns4x4 - column4x4,
                                  DivideBy4(window_buffer_width_));
        copy_width = MultiplyBy4(copy_width) >> subsampling_x;
        int copy_height =
            std::min(frame_header_.rows4x4 - row4x4, window_buffer_height4x4);
        copy_height = MultiplyBy4(copy_height) >> subsampling_y;
        CopyPlane<Pixel>(
            threaded_window_buffer_ + plane * window_buffer_plane_size,
            window_buffer_width_ * pixel_size_, copy_width, copy_height,
            source_buffer_->data(plane) + plane_row * src_stride +
                plane_column * pixel_size_ + buffer_offset,
            src_stride);
      }
    }
  }
  for (int plane = kPlaneY; plane < planes_; ++plane) {
    source_buffer_->ShiftBuffer(plane, horizontal_shift, vertical_shift);
  }
}

void PostFilter::ApplySuperResForOneSuperBlockRow(int row4x4_start, int sb4x4) {
  assert(row4x4_start >= 0);
  assert(DoSuperRes());
  const int horizontal_shift = -source_buffer_->alignment();
  const int vertical_shift = -kCdefBorder;
  std::array<ptrdiff_t, kMaxPlanes> plane_offsets;
  for (int plane = 0; plane < planes_; ++plane) {
    // If cdef is on, then the output of cdef was written with a shift to allow
    // in-place cdef filtering.
    const ptrdiff_t cdef_offset =
        DoCdef() ? vertical_shift * (source_buffer_->stride(plane)) +
                       horizontal_shift * pixel_size_
                 : 0;
    const int subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
    const ptrdiff_t row_offset = (MultiplyBy4(row4x4_start) >> subsampling_y) *
                                 source_buffer_->stride(plane);
    plane_offsets[plane] = cdef_offset + row_offset;
  }
  const int num_rows4x4 = std::min(sb4x4, frame_header_.rows4x4 - row4x4_start);
  ApplySuperRes(source_buffer_, num_rows4x4, subsampling_y_, plane_offsets);
}

void PostFilter::ApplyCdefForOneSuperBlockRow(int row4x4_start, int sb4x4) {
  assert(row4x4_start >= 0);
  assert(DoCdef());
  static constexpr int step_64x64 = 16;
  for (int y = 0; y < sb4x4; y += step_64x64) {
    const int row4x4 = row4x4_start + y;
    if (row4x4 >= frame_header_.rows4x4) break;
    for (int column4x4 = 0; column4x4 < frame_header_.columns4x4;
         column4x4 += step_64x64) {
      const int index = cdef_index_[DivideBy16(row4x4)][DivideBy16(column4x4)];
      const int block_width4x4 =
          std::min(step_64x64, frame_header_.columns4x4 - column4x4);
      const int block_height4x4 =
          std::min(step_64x64, frame_header_.rows4x4 - row4x4);

#if LIBGAV1_MAX_BITDEPTH >= 10
      if (bitdepth_ >= 10) {
        ApplyCdefForOneUnit<uint16_t>(cdef_block_, index, block_width4x4,
                                      block_height4x4, row4x4, column4x4);
        continue;
      }
#endif  // LIBGAV1_MAX_BITDEPTH >= 10
      ApplyCdefForOneUnit<uint8_t>(cdef_block_, index, block_width4x4,
                                   block_height4x4, row4x4, column4x4);
    }
  }
}

void PostFilter::ApplyCdef() {
#if LIBGAV1_MAX_BITDEPTH >= 10
  if (bitdepth_ >= 10) {
    ApplyCdefThreaded<uint16_t>();
    return;
  }
#endif
  ApplyCdefThreaded<uint8_t>();
}

void PostFilter::ApplySuperRes(
    YuvBuffer* const buffer, int rows4x4, int8_t chroma_subsampling_y,
    const std::array<ptrdiff_t, kMaxPlanes>& plane_offsets) {
  uint8_t* const line_buffer_start =
      superres_line_buffer_ + kSuperResBorder * pixel_size_;
  for (int plane = kPlaneY; plane < planes_; ++plane) {
    const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
    const int8_t subsampling_y = (plane == kPlaneY) ? 0 : chroma_subsampling_y;
    const int plane_width =
        MultiplyBy4(frame_header_.columns4x4) >> subsampling_x;
    const int plane_height = MultiplyBy4(rows4x4) >> subsampling_y;
    const int downscaled_width = RightShiftWithRounding(width_, subsampling_x);
    const int upscaled_width =
        RightShiftWithRounding(upscaled_width_, subsampling_x);
    const int superres_width = downscaled_width << kSuperResScaleBits;
    const int step = (superres_width + upscaled_width / 2) / upscaled_width;
    const int error = step * upscaled_width - superres_width;
    int initial_subpixel_x =
        (-((upscaled_width - downscaled_width) << (kSuperResScaleBits - 1)) +
         DivideBy2(upscaled_width)) /
            upscaled_width +
        (1 << (kSuperResExtraBits - 1)) - error / 2;
    initial_subpixel_x &= kSuperResScaleMask;
    uint8_t* input = buffer->data(plane) + plane_offsets[plane];
    const uint32_t input_stride = buffer->stride(plane);
    for (int y = 0; y < plane_height; ++y, input += input_stride) {
#if LIBGAV1_MAX_BITDEPTH >= 10
      if (bitdepth_ >= 10) {
        CopyPlane<uint16_t>(input, input_stride, plane_width, 1,
                            line_buffer_start, /*dest_stride=*/0);
        ExtendLine<uint16_t>(line_buffer_start, plane_width, kSuperResBorder,
                             kSuperResBorder);
        ComputeSuperRes<10, uint16_t>(line_buffer_start, upscaled_width,
                                      initial_subpixel_x, step, input);
        continue;
      }
#endif  // LIBGAV1_MAX_BITDEPTH >= 10
      CopyPlane<uint8_t>(input, input_stride, plane_width, 1, line_buffer_start,
                         /*dest_stride=*/0);
      ExtendLine<uint8_t>(line_buffer_start, plane_width, kSuperResBorder,
                          kSuperResBorder);
      ComputeSuperRes<8, uint8_t>(line_buffer_start, upscaled_width,
                                  initial_subpixel_x, step, input);
    }
  }
}

template <typename Pixel>
void PostFilter::ApplyLoopRestorationForOneRowInWindow(
    uint8_t* const cdef_buffer, const ptrdiff_t cdef_buffer_stride,
    const Plane plane, const int plane_height, const int plane_width,
    const int x, const int y, const int row, const int unit_row,
    const int current_process_unit_height, const int process_unit_width,
    const int window_width, const int plane_unit_size,
    const int num_horizontal_units) {
  Array2DView<Pixel> loop_restored_window(
      window_buffer_height_, window_buffer_width_,
      reinterpret_cast<Pixel*>(threaded_window_buffer_));
  for (int column = 0; column < window_width; column += process_unit_width) {
    ApplyLoopRestorationForOneUnit<Pixel>(
        cdef_buffer, cdef_buffer_stride, plane, plane_height, x, y, row, column,
        unit_row, current_process_unit_height, process_unit_width,
        plane_unit_size, num_horizontal_units, plane_width,
        &loop_restored_window);
  }
}

void PostFilter::CopyBorderForRestoration(int row4x4, int sb4x4) {
  assert(row4x4 >= 0);
  assert(DoRestoration());
  for (int plane = 0; plane < planes_; ++plane) {
    int subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
    int subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
    const int row = MultiplyBy4(row4x4) >> subsampling_y;
    const int plane_width =
        RightShiftWithRounding(upscaled_width_, subsampling_x);
    const int plane_height = RightShiftWithRounding(
        std::min(MultiplyBy4(sb4x4), height_ - MultiplyBy4(row4x4)),
        subsampling_y);
    const bool copy_bottom = row4x4 + sb4x4 >= frame_header_.rows4x4;
    const int cdef_horizontal_shift = -source_buffer_->alignment();
    const int cdef_vertical_shift = -kCdefBorder;
    const ptrdiff_t cdef_buffer_offset =
        DoCdef() ? cdef_vertical_shift * source_buffer_->stride(plane) +
                       cdef_horizontal_shift * pixel_size_
                 : 0;
    uint8_t* const start =
        SetBufferOffset(source_buffer_, static_cast<Plane>(plane), row4x4, 0,
                        subsampling_x, subsampling_y) +
        cdef_buffer_offset;
    const ptrdiff_t stride = source_buffer_->stride(plane);
#if LIBGAV1_MAX_BITDEPTH >= 10
    if (bitdepth_ >= 10) {
      ExtendFrame<uint16_t>(start, plane_width, plane_height, stride,
                            kRestorationBorder, kRestorationBorder,
                            (row == 0) ? kRestorationBorder : 0,
                            copy_bottom ? kRestorationBorder : 0);
      continue;
    }
#endif
    ExtendFrame<uint8_t>(start, plane_width, plane_height, stride,
                         kRestorationBorder, kRestorationBorder,
                         (row == 0) ? kRestorationBorder : 0,
                         copy_bottom ? kRestorationBorder : 0);
  }
}

void PostFilter::ApplyLoopRestorationForOneSuperBlockRow(int row4x4_start,
                                                         int sb4x4) {
  assert(row4x4_start >= 0);
  assert(DoRestoration());
  const int plane_process_unit_width[kMaxPlanes] = {
      kRestorationProcessingUnitSize,
      kRestorationProcessingUnitSize >> subsampling_x_,
      kRestorationProcessingUnitSize >> subsampling_x_};
  const int plane_process_unit_height[kMaxPlanes] = {
      kRestorationProcessingUnitSize,
      kRestorationProcessingUnitSize >> subsampling_y_,
      kRestorationProcessingUnitSize >> subsampling_y_};
  for (int plane = 0; plane < planes_; ++plane) {
    if (frame_header_.loop_restoration.type[plane] ==
        kLoopRestorationTypeNone) {
      continue;
    }
    const int subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
    const int subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
    const int unit_height_offset = kRestorationUnitOffset >> subsampling_y;
    const int plane_height =
        RightShiftWithRounding(frame_header_.height, subsampling_y);
    const int plane_width =
        RightShiftWithRounding(frame_header_.upscaled_width, subsampling_x);
    const int num_vertical_units =
        restoration_info_->num_vertical_units(static_cast<Plane>(plane));
    const int process_unit_width = plane_process_unit_width[plane];
    for (int sb_y = 0; sb_y < sb4x4; sb_y += 16) {
      const int row4x4 = row4x4_start + sb_y;
      const int y =
          (MultiplyBy4(row4x4) - (row4x4 == 0 ? 0 : 8)) >> subsampling_y;
      if (y >= plane_height) break;
      const int plane_unit_size =
          frame_header_.loop_restoration.unit_size[plane];
      const int unit_row = std::min((y + unit_height_offset) / plane_unit_size,
                                    num_vertical_units - 1);
      const int expected_height = plane_process_unit_height[plane] +
                                  ((y == 0) ? -unit_height_offset : 0);
      const int current_process_unit_height =
          (y + expected_height <= plane_height) ? expected_height
                                                : plane_height - y;
      for (int column4x4 = 0;; column4x4 += 16) {
        const int x = MultiplyBy4(column4x4) >> subsampling_x;
        if (x >= plane_width) break;
#if LIBGAV1_MAX_BITDEPTH >= 10
        if (bitdepth_ >= 10) {
          ApplyLoopRestorationForSuperBlock<uint16_t>(
              static_cast<Plane>(plane), x, y, unit_row,
              current_process_unit_height, process_unit_width);
          continue;
        }
#endif
        ApplyLoopRestorationForSuperBlock<uint8_t>(
            static_cast<Plane>(plane), x, y, unit_row,
            current_process_unit_height, process_unit_width);
      }
    }
  }
}

template <typename Pixel>
void PostFilter::ApplyLoopRestorationForSuperBlock(
    const Plane plane, const int x, const int y, const int unit_row,
    const int current_process_unit_height, const int process_unit_width) {
  uint8_t* const src_buffer = source_buffer_->data(plane);
  const int src_stride = source_buffer_->stride(plane);
  const int cdef_horizontal_shift = -source_buffer_->alignment();
  const int cdef_vertical_shift = -kCdefBorder;
  const ptrdiff_t cdef_buffer_offset =
      DoCdef() ? cdef_vertical_shift * src_stride +
                     cdef_horizontal_shift * pixel_size_
               : 0;
  // The input to loop restoration is the cdef filtered pixels. If cdef
  // filtering is on, then the output is written with a shift. So the source
  // buffer has to be offset by that shift.
  uint8_t* const cdef_buffer = source_buffer_->data(plane) + cdef_buffer_offset;
  const int plane_unit_size = loop_restoration_.unit_size[plane];
  const int num_horizontal_units =
      restoration_info_->num_horizontal_units(static_cast<Plane>(plane));
  const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
  const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
  const int plane_width =
      RightShiftWithRounding(upscaled_width_, subsampling_x);
  const int plane_height = RightShiftWithRounding(height_, subsampling_y);
  const int horizontal_shift = -source_buffer_->alignment();
  const int vertical_shift = -kRestorationBorder;
  const ptrdiff_t src_unit_buffer_offset = vertical_shift * src_stride +
                                           horizontal_shift * pixel_size_ +
                                           cdef_buffer_offset;
  // The output of loop restoration is written into the source frame buffer with
  // a shift to the top left.
  Array2DView<Pixel> loop_restored_window(
      source_buffer_->aligned_height(plane), src_stride / sizeof(Pixel),
      reinterpret_cast<Pixel*>(src_buffer + y * src_stride + x * pixel_size_ +
                               src_unit_buffer_offset));
  ApplyLoopRestorationForOneUnit<Pixel>(
      cdef_buffer, src_stride, plane, plane_height, x, y, 0, 0, unit_row,
      current_process_unit_height, process_unit_width, plane_unit_size,
      num_horizontal_units, plane_width, &loop_restored_window);
}

template <typename Pixel>
void PostFilter::ApplyLoopRestorationForOneUnit(
    uint8_t* const cdef_buffer, const ptrdiff_t cdef_buffer_stride,
    const Plane plane, const int plane_height, const int x, const int y,
    const int row, const int column, const int unit_row,
    const int current_process_unit_height, const int plane_process_unit_width,
    const int plane_unit_size, const int num_horizontal_units,
    const int plane_width, Array2DView<Pixel>* const loop_restored_window) {
  const int unit_x = x + column;
  const int unit_y = y + row;
  const int current_process_unit_width =
      (unit_x + plane_process_unit_width <= plane_width)
          ? plane_process_unit_width
          : plane_width - unit_x;
  uint8_t* cdef_unit_buffer =
      cdef_buffer + unit_y * cdef_buffer_stride + unit_x * pixel_size_;
  const int unit_column =
      std::min(unit_x / plane_unit_size, num_horizontal_units - 1);
  const int unit_id = unit_row * num_horizontal_units + unit_column;
  const LoopRestorationType type =
      restoration_info_
          ->loop_restoration_info(static_cast<Plane>(plane), unit_id)
          .type;
  if (type == kLoopRestorationTypeNone) {
    Pixel* dest = &(*loop_restored_window)[row][column];
    for (int k = 0; k < current_process_unit_height; ++k) {
      memcpy(dest, cdef_unit_buffer, current_process_unit_width * pixel_size_);
      dest += loop_restored_window->columns();
      cdef_unit_buffer += cdef_buffer_stride;
    }
    return;
  }

  // The SIMD implementation of wiener filter (currently WienerFilter_SSE4_1())
  // over-reads 6 bytes, so add 6 extra bytes at the end of block_buffer for 8
  // bit.
  alignas(alignof(uint16_t))
      uint8_t block_buffer[kRestorationProcessingUnitSizeWithBorders *
                               kRestorationProcessingUnitSizeWithBorders *
                               sizeof(Pixel) +
                           ((sizeof(Pixel) == 1) ? 6 : 0)];
  const ptrdiff_t block_buffer_stride =
      kRestorationProcessingUnitSizeWithBorders * pixel_size_;
  IntermediateBuffers intermediate_buffers;

  RestorationBuffer restoration_buffer = {
      {intermediate_buffers.box_filter.output[0],
       intermediate_buffers.box_filter.output[1]},
      plane_process_unit_width,
      {intermediate_buffers.box_filter.intermediate_a,
       intermediate_buffers.box_filter.intermediate_b},
      kRestorationProcessingUnitSizeWithBorders + kRestorationPadding,
      intermediate_buffers.wiener,
      kMaxSuperBlockSizeInPixels};
  const int deblock_buffer_units =
      64 >> (plane == kPlaneY ? 0 : subsampling_y_);
  uint8_t* const deblock_buffer = deblock_buffer_.data(plane);
  const int deblock_buffer_stride = deblock_buffer_.stride(plane);
  const int deblock_unit_y = MultiplyBy4(Ceil(unit_y, deblock_buffer_units));
  uint8_t* deblock_unit_buffer =
      (deblock_buffer != nullptr)
          ? deblock_buffer + deblock_unit_y * deblock_buffer_stride +
                unit_x * pixel_size_
          : nullptr;
  assert(type == kLoopRestorationTypeSgrProj ||
         type == kLoopRestorationTypeWiener);
  const dsp::LoopRestorationFunc restoration_func =
      dsp_.loop_restorations[type - 2];
  PrepareLoopRestorationBlock<Pixel>(
      DoCdef(), cdef_unit_buffer, cdef_buffer_stride, deblock_unit_buffer,
      deblock_buffer_stride, block_buffer, block_buffer_stride,
      current_process_unit_width, current_process_unit_height, unit_y == 0,
      unit_y + current_process_unit_height >= plane_height);
  restoration_func(reinterpret_cast<const uint8_t*>(
                       block_buffer + kRestorationBorder * block_buffer_stride +
                       kRestorationBorder * pixel_size_),
                   &(*loop_restored_window)[row][column],
                   restoration_info_->loop_restoration_info(
                       static_cast<Plane>(plane), unit_id),
                   block_buffer_stride,
                   loop_restored_window->columns() * pixel_size_,
                   current_process_unit_width, current_process_unit_height,
                   &restoration_buffer);
}

// Multi-thread version of loop restoration, based on a moving window of size
// |window_buffer_width_|x|window_buffer_height_|. Inside the moving window, we
// create a filtering job for each row and each filtering job is submitted to
// the thread pool. Each free thread takes one job from the thread pool and
// completes filtering until all jobs are finished. This approach requires an
// extra buffer (|threaded_window_buffer_|) to hold the filtering output, whose
// size is the size of the window. It also needs block buffers (i.e.,
// |block_buffer| and |intermediate_buffers| in
// ApplyLoopRestorationForOneUnit()) to store intermediate results in loop
// restoration for each thread. After all units inside the window are filtered,
// the output is written to the frame buffer.
template <typename Pixel>
void PostFilter::ApplyLoopRestorationThreaded() {
  const int plane_process_unit_width[kMaxPlanes] = {
      kRestorationProcessingUnitSize,
      kRestorationProcessingUnitSize >> subsampling_x_,
      kRestorationProcessingUnitSize >> subsampling_x_};
  const int plane_process_unit_height[kMaxPlanes] = {
      kRestorationProcessingUnitSize,
      kRestorationProcessingUnitSize >> subsampling_y_,
      kRestorationProcessingUnitSize >> subsampling_y_};

  // abs(horizontal_shift) must be at least kRestorationBorder. Increase it to
  // preserve buffer alignment.
  const int horizontal_shift = -source_buffer_->alignment();
  const int vertical_shift = -kRestorationBorder;
  for (int plane = kPlaneY; plane < planes_; ++plane) {
    if (loop_restoration_.type[plane] == kLoopRestorationTypeNone) {
      continue;
    }

    const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
    const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
    const int unit_height_offset = kRestorationUnitOffset >> subsampling_y;
    uint8_t* const src_buffer = source_buffer_->data(plane);
    const int src_stride = source_buffer_->stride(plane);
    const int plane_unit_size = loop_restoration_.unit_size[plane];
    const int num_vertical_units =
        restoration_info_->num_vertical_units(static_cast<Plane>(plane));
    const int num_horizontal_units =
        restoration_info_->num_horizontal_units(static_cast<Plane>(plane));
    const int plane_width =
        RightShiftWithRounding(upscaled_width_, subsampling_x);
    const int plane_height = RightShiftWithRounding(height_, subsampling_y);
    const ptrdiff_t src_unit_buffer_offset =
        vertical_shift * src_stride + horizontal_shift * pixel_size_;
    ExtendFrameBoundary(src_buffer, plane_width, plane_height, src_stride,
                        kRestorationBorder, kRestorationBorder,
                        kRestorationBorder, kRestorationBorder);

    const int num_workers = thread_pool_->num_threads();
    for (int y = 0; y < plane_height; y += window_buffer_height_) {
      const int actual_window_height =
          std::min(window_buffer_height_ - ((y == 0) ? unit_height_offset : 0),
                   plane_height - y);
      int vertical_units_per_window =
          (actual_window_height + plane_process_unit_height[plane] - 1) /
          plane_process_unit_height[plane];
      if (y == 0) {
        // The first row of loop restoration processing units is not 64x64, but
        // 64x56 (|unit_height_offset| = 8 rows less than other restoration
        // processing units). For u/v with subsampling, the size is halved. To
        // compute the number of vertical units per window, we need to take a
        // special handling for it.
        const int height_without_first_unit =
            actual_window_height -
            std::min(actual_window_height,
                     plane_process_unit_height[plane] - unit_height_offset);
        vertical_units_per_window =
            (height_without_first_unit + plane_process_unit_height[plane] - 1) /
                plane_process_unit_height[plane] +
            1;
      }
      for (int x = 0; x < plane_width; x += window_buffer_width_) {
        const int actual_window_width =
            std::min(window_buffer_width_, plane_width - x);
        const int jobs_for_threadpool =
            vertical_units_per_window * num_workers / (num_workers + 1);
        assert(jobs_for_threadpool < vertical_units_per_window);
        BlockingCounter pending_jobs(jobs_for_threadpool);
        int job_count = 0;
        int current_process_unit_height;
        for (int row = 0; row < actual_window_height;
             row += current_process_unit_height) {
          const int unit_y = y + row;
          const int expected_height = plane_process_unit_height[plane] +
                                      ((unit_y == 0) ? -unit_height_offset : 0);
          current_process_unit_height =
              (unit_y + expected_height <= plane_height)
                  ? expected_height
                  : plane_height - unit_y;
          const int unit_row =
              std::min((unit_y + unit_height_offset) / plane_unit_size,
                       num_vertical_units - 1);
          const int process_unit_width = plane_process_unit_width[plane];

          if (job_count < jobs_for_threadpool) {
            thread_pool_->Schedule(
                [this, src_buffer, src_stride, process_unit_width,
                 current_process_unit_height, actual_window_width,
                 plane_unit_size, num_horizontal_units, x, y, row, unit_row,
                 plane_height, plane_width, plane, &pending_jobs]() {
                  ApplyLoopRestorationForOneRowInWindow<Pixel>(
                      src_buffer, src_stride, static_cast<Plane>(plane),
                      plane_height, plane_width, x, y, row, unit_row,
                      current_process_unit_height, process_unit_width,
                      actual_window_width, plane_unit_size,
                      num_horizontal_units);
                  pending_jobs.Decrement();
                });
          } else {
            ApplyLoopRestorationForOneRowInWindow<Pixel>(
                src_buffer, src_stride, static_cast<Plane>(plane), plane_height,
                plane_width, x, y, row, unit_row, current_process_unit_height,
                process_unit_width, actual_window_width, plane_unit_size,
                num_horizontal_units);
          }
          ++job_count;
        }
        // Wait for all jobs of current window to finish.
        pending_jobs.Wait();
        // Copy |threaded_window_buffer_| to output frame.
        CopyPlane<Pixel>(threaded_window_buffer_,
                         window_buffer_width_ * pixel_size_,
                         actual_window_width, actual_window_height,
                         src_buffer + y * src_stride + x * pixel_size_ +
                             src_unit_buffer_offset,
                         src_stride);
      }
      if (y == 0) y -= unit_height_offset;
    }
    source_buffer_->ShiftBuffer(plane, horizontal_shift, vertical_shift);
  }
}

void PostFilter::ApplyLoopRestoration() {
  assert(threaded_window_buffer_ != nullptr);
#if LIBGAV1_MAX_BITDEPTH >= 10
  if (bitdepth_ >= 10) {
    ApplyLoopRestorationThreaded<uint16_t>();
    return;
  }
#endif
  ApplyLoopRestorationThreaded<uint8_t>();
}

void PostFilter::HorizontalDeblockFilter(Plane plane, int row4x4_start,
                                         int column4x4_start, int unit_id) {
  const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
  const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
  const int row_step = 1 << subsampling_y;
  const int column_step = 1 << subsampling_x;
  const size_t src_step = 4 * pixel_size_;
  const ptrdiff_t row_stride = MultiplyBy4(source_buffer_->stride(plane));
  const ptrdiff_t src_stride = source_buffer_->stride(plane);
  uint8_t* src = SetBufferOffset(source_buffer_, plane, row4x4_start,
                                 column4x4_start, subsampling_x, subsampling_y);
  const uint64_t single_row_mask = 0xffff;
  // 3 (11), 5 (0101).
  const uint64_t two_block_mask = (subsampling_x > 0) ? 5 : 3;
  const LoopFilterType type = kLoopFilterTypeHorizontal;
  // Subsampled UV samples correspond to the right/bottom position of
  // Y samples.
  const int column = subsampling_x;

  // AV1 smallest transform size is 4x4, thus minimum horizontal edge size is
  // 4x4. For SIMD implementation, sse2 could compute 8 pixels at the same time.
  // __m128i = 8 x uint16_t, AVX2 could compute 16 pixels at the same time.
  // __m256i = 16 x uint16_t, assuming pixel type is 16 bit. It means we could
  // filter 2 horizontal edges using sse2 and 4 edges using AVX2.
  // The bitmask enables us to call different SIMD implementations to filter
  // 1 edge, or 2 edges or 4 edges.
  // TODO(chengchen): Here, the implementation only consider 1 and 2 edges.
  // Add support for 4 edges. More branches involved, for example, if input is
  // 8 bit, __m128i = 16 x 8 bit, we could apply filtering for 4 edges using
  // sse2, 8 edges using AVX2. If input is 16 bit, __m128 = 8 x 16 bit, then
  // we apply filtering for 2 edges using sse2, and 4 edges using AVX2.
  for (int row4x4 = 0; MultiplyBy4(row4x4_start + row4x4) < height_ &&
                       row4x4 < kNum4x4InLoopFilterMaskUnit;
       row4x4 += row_step) {
    if (row4x4_start + row4x4 == 0) {
      src += row_stride;
      continue;
    }
    // Subsampled UV samples correspond to the right/bottom position of
    // Y samples.
    const int row = GetDeblockPosition(row4x4, subsampling_y);
    const int index = GetIndex(row);
    const int shift = GetShift(row, column);
    const int level_offset = LoopFilterMask::GetLevelOffset(row, column);
    // Mask of current row. mask4x4 represents the vertical filter length for
    // the current horizontal edge is 4, and we needs to apply 3-tap filtering.
    // Similarly, mask8x8 and mask16x16 represent filter lengths are 8 and 16.
    uint64_t mask4x4 =
        (masks_->GetTop(unit_id, plane, kLoopFilterTransformSizeId4x4, index) >>
         shift) &
        single_row_mask;
    uint64_t mask8x8 =
        (masks_->GetTop(unit_id, plane, kLoopFilterTransformSizeId8x8, index) >>
         shift) &
        single_row_mask;
    uint64_t mask16x16 =
        (masks_->GetTop(unit_id, plane, kLoopFilterTransformSizeId16x16,
                        index) >>
         shift) &
        single_row_mask;
    // mask4x4, mask8x8, mask16x16 are mutually exclusive.
    assert((mask4x4 & mask8x8) == 0 && (mask4x4 & mask16x16) == 0 &&
           (mask8x8 & mask16x16) == 0);
    // Apply deblock filter for one row.
    uint8_t* src_row = src;
    int column_offset = 0;
    for (uint64_t mask = mask4x4 | mask8x8 | mask16x16; mask != 0;) {
      int edge_count = 1;
      if ((mask & 1) != 0) {
        // Filter parameters of current edge.
        const uint8_t level = masks_->GetLevel(unit_id, plane, type,
                                               level_offset + column_offset);
        int outer_thresh_0;
        int inner_thresh_0;
        int hev_thresh_0;
        GetDeblockFilterParams(level, &outer_thresh_0, &inner_thresh_0,
                               &hev_thresh_0);
        // Filter parameters of next edge. Clip the index to avoid over
        // reading at the edge of the block. The values will be unused in that
        // case.
        const int level_next_index = level_offset + column_offset + column_step;
        const uint8_t level_next =
            masks_->GetLevel(unit_id, plane, type, level_next_index & 0xff);
        int outer_thresh_1;
        int inner_thresh_1;
        int hev_thresh_1;
        GetDeblockFilterParams(level_next, &outer_thresh_1, &inner_thresh_1,
                               &hev_thresh_1);

        if ((mask16x16 & 1) != 0) {
          const dsp::LoopFilterSize size = (plane == kPlaneY)
                                               ? dsp::kLoopFilterSize14
                                               : dsp::kLoopFilterSize6;
          const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
          if ((mask16x16 & two_block_mask) == two_block_mask) {
            edge_count = 2;
            // Apply filtering for two edges.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
            filter_func(src_row + src_step, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          } else {
            // Apply single edge filtering.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
          }
        }

        if ((mask8x8 & 1) != 0) {
          const dsp::LoopFilterSize size =
              plane == kPlaneY ? dsp::kLoopFilterSize8 : dsp::kLoopFilterSize6;
          const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
          if ((mask8x8 & two_block_mask) == two_block_mask) {
            edge_count = 2;
            // Apply filtering for two edges.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
            filter_func(src_row + src_step, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          } else {
            // Apply single edge filtering.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
          }
        }

        if ((mask4x4 & 1) != 0) {
          const dsp::LoopFilterSize size = dsp::kLoopFilterSize4;
          const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
          if ((mask4x4 & two_block_mask) == two_block_mask) {
            edge_count = 2;
            // Apply filtering for two edges.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
            filter_func(src_row + src_step, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          } else {
            // Apply single edge filtering.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
          }
        }
      }

      const int step = edge_count * column_step;
      mask4x4 >>= step;
      mask8x8 >>= step;
      mask16x16 >>= step;
      mask >>= step;
      column_offset += step;
      src_row += MultiplyBy4(edge_count) * pixel_size_;
    }
    src += row_stride;
  }
}

void PostFilter::VerticalDeblockFilter(Plane plane, int row4x4_start,
                                       int column4x4_start, int unit_id) {
  const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
  const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
  const int row_step = 1 << subsampling_y;
  const int two_row_step = row_step << 1;
  const int column_step = 1 << subsampling_x;
  const size_t src_step = (bitdepth_ == 8) ? 4 : 4 * sizeof(uint16_t);
  const ptrdiff_t row_stride = MultiplyBy4(source_buffer_->stride(plane));
  const ptrdiff_t two_row_stride = row_stride << 1;
  const ptrdiff_t src_stride = source_buffer_->stride(plane);
  uint8_t* src = SetBufferOffset(source_buffer_, plane, row4x4_start,
                                 column4x4_start, subsampling_x, subsampling_y);
  const uint64_t single_row_mask = 0xffff;
  const LoopFilterType type = kLoopFilterTypeVertical;
  // Subsampled UV samples correspond to the right/bottom position of
  // Y samples.
  const int column = subsampling_x;

  // AV1 smallest transform size is 4x4, thus minimum vertical edge size is 4x4.
  // For SIMD implementation, sse2 could compute 8 pixels at the same time.
  // __m128i = 8 x uint16_t, AVX2 could compute 16 pixels at the same time.
  // __m256i = 16 x uint16_t, assuming pixel type is 16 bit. It means we could
  // filter 2 vertical edges using sse2 and 4 edges using AVX2.
  // The bitmask enables us to call different SIMD implementations to filter
  // 1 edge, or 2 edges or 4 edges.
  // TODO(chengchen): Here, the implementation only consider 1 and 2 edges.
  // Add support for 4 edges. More branches involved, for example, if input is
  // 8 bit, __m128i = 16 x 8 bit, we could apply filtering for 4 edges using
  // sse2, 8 edges using AVX2. If input is 16 bit, __m128 = 8 x 16 bit, then
  // we apply filtering for 2 edges using sse2, and 4 edges using AVX2.
  for (int row4x4 = 0; MultiplyBy4(row4x4_start + row4x4) < height_ &&
                       row4x4 < kNum4x4InLoopFilterMaskUnit;
       row4x4 += two_row_step) {
    // Subsampled UV samples correspond to the right/bottom position of
    // Y samples.
    const int row = GetDeblockPosition(row4x4, subsampling_y);
    const int row_next = row + row_step;
    const int index = GetIndex(row);
    const int shift = GetShift(row, column);
    const int level_offset = LoopFilterMask::GetLevelOffset(row, column);
    const int index_next = GetIndex(row_next);
    const int shift_next_row = GetShift(row_next, column);
    const int level_offset_next_row =
        LoopFilterMask::GetLevelOffset(row_next, column);
    // TODO(chengchen): replace 0, 1, 2 to meaningful enum names.
    // mask of current row. mask4x4 represents the horizontal filter length for
    // the current vertical edge is 4, and we needs to apply 3-tap filtering.
    // Similarly, mask8x8 and mask16x16 represent filter lengths are 8 and 16.
    uint64_t mask4x4_0 =
        (masks_->GetLeft(unit_id, plane, kLoopFilterTransformSizeId4x4,
                         index) >>
         shift) &
        single_row_mask;
    uint64_t mask8x8_0 =
        (masks_->GetLeft(unit_id, plane, kLoopFilterTransformSizeId8x8,
                         index) >>
         shift) &
        single_row_mask;
    uint64_t mask16x16_0 =
        (masks_->GetLeft(unit_id, plane, kLoopFilterTransformSizeId16x16,
                         index) >>
         shift) &
        single_row_mask;
    // mask4x4, mask8x8, mask16x16 are mutually exclusive.
    assert((mask4x4_0 & mask8x8_0) == 0 && (mask4x4_0 & mask16x16_0) == 0 &&
           (mask8x8_0 & mask16x16_0) == 0);
    // mask of the next row. With mask of current and the next row, we can call
    // the corresponding SIMD function to apply filtering for two vertical
    // edges together.
    uint64_t mask4x4_1 =
        (masks_->GetLeft(unit_id, plane, kLoopFilterTransformSizeId4x4,
                         index_next) >>
         shift_next_row) &
        single_row_mask;
    uint64_t mask8x8_1 =
        (masks_->GetLeft(unit_id, plane, kLoopFilterTransformSizeId8x8,
                         index_next) >>
         shift_next_row) &
        single_row_mask;
    uint64_t mask16x16_1 =
        (masks_->GetLeft(unit_id, plane, kLoopFilterTransformSizeId16x16,
                         index_next) >>
         shift_next_row) &
        single_row_mask;
    // mask4x4, mask8x8, mask16x16 are mutually exclusive.
    assert((mask4x4_1 & mask8x8_1) == 0 && (mask4x4_1 & mask16x16_1) == 0 &&
           (mask8x8_1 & mask16x16_1) == 0);
    // Apply deblock filter for two rows.
    uint8_t* src_row = src;
    int column_offset = 0;
    for (uint64_t mask = mask4x4_0 | mask8x8_0 | mask16x16_0 | mask4x4_1 |
                         mask8x8_1 | mask16x16_1;
         mask != 0;) {
      if ((mask & 1) != 0) {
        // Filter parameters of current row.
        const uint8_t level = masks_->GetLevel(unit_id, plane, type,
                                               level_offset + column_offset);
        int outer_thresh_0;
        int inner_thresh_0;
        int hev_thresh_0;
        GetDeblockFilterParams(level, &outer_thresh_0, &inner_thresh_0,
                               &hev_thresh_0);
        // Filter parameters of next row. Clip the index to avoid over
        // reading at the edge of the block. The values will be unused in that
        // case.
        const int level_next_index = level_offset_next_row + column_offset;
        const uint8_t level_next =
            masks_->GetLevel(unit_id, plane, type, level_next_index & 0xff);
        int outer_thresh_1;
        int inner_thresh_1;
        int hev_thresh_1;
        GetDeblockFilterParams(level_next, &outer_thresh_1, &inner_thresh_1,
                               &hev_thresh_1);
        uint8_t* const src_row_next = src_row + row_stride;

        if (((mask16x16_0 | mask16x16_1) & 1) != 0) {
          const dsp::LoopFilterSize size = (plane == kPlaneY)
                                               ? dsp::kLoopFilterSize14
                                               : dsp::kLoopFilterSize6;
          const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
          if ((mask16x16_0 & mask16x16_1 & 1) != 0) {
            // Apply dual vertical edge filtering.
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
            filter_func(src_row_next, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          } else if ((mask16x16_0 & 1) != 0) {
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
          } else {
            filter_func(src_row_next, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          }
        }

        if (((mask8x8_0 | mask8x8_1) & 1) != 0) {
          const dsp::LoopFilterSize size = (plane == kPlaneY)
                                               ? dsp::kLoopFilterSize8
                                               : dsp::kLoopFilterSize6;
          const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
          if ((mask8x8_0 & mask8x8_1 & 1) != 0) {
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
            filter_func(src_row_next, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          } else if ((mask8x8_0 & 1) != 0) {
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
          } else {
            filter_func(src_row_next, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          }
        }

        if (((mask4x4_0 | mask4x4_1) & 1) != 0) {
          const dsp::LoopFilterSize size = dsp::kLoopFilterSize4;
          const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
          if ((mask4x4_0 & mask4x4_1 & 1) != 0) {
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
            filter_func(src_row_next, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          } else if ((mask4x4_0 & 1) != 0) {
            filter_func(src_row, src_stride, outer_thresh_0, inner_thresh_0,
                        hev_thresh_0);
          } else {
            filter_func(src_row_next, src_stride, outer_thresh_1,
                        inner_thresh_1, hev_thresh_1);
          }
        }
      }

      mask4x4_0 >>= column_step;
      mask8x8_0 >>= column_step;
      mask16x16_0 >>= column_step;
      mask4x4_1 >>= column_step;
      mask8x8_1 >>= column_step;
      mask16x16_1 >>= column_step;
      mask >>= column_step;
      column_offset += column_step;
      src_row += src_step;
    }
    src += two_row_stride;
  }
}

void PostFilter::HorizontalDeblockFilterNoMask(Plane plane, int row4x4_start,
                                               int column4x4_start,
                                               int unit_id) {
  static_cast<void>(unit_id);
  const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
  const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
  const int column_step = 1 << subsampling_x;
  const size_t src_step = MultiplyBy4(pixel_size_);
  const ptrdiff_t src_stride = source_buffer_->stride(plane);
  uint8_t* src = SetBufferOffset(source_buffer_, plane, row4x4_start,
                                 column4x4_start, subsampling_x, subsampling_y);
  const LoopFilterType type = kLoopFilterTypeHorizontal;
  int row_step;
  uint8_t level;
  int filter_length;

  for (int column4x4 = 0; MultiplyBy4(column4x4_start + column4x4) < width_ &&
                          column4x4 < kNum4x4InLoopFilterMaskUnit;
       column4x4 += column_step, src += src_step) {
    uint8_t* src_row = src;
    for (int row4x4 = 0; MultiplyBy4(row4x4_start + row4x4) < height_ &&
                         row4x4 < kNum4x4InLoopFilterMaskUnit;
         row4x4 += row_step) {
      const bool need_filter = GetDeblockFilterEdgeInfo(
          plane, row4x4_start + row4x4, column4x4_start + column4x4,
          subsampling_x, subsampling_y, type, &level, &row_step,
          &filter_length);
      if (need_filter) {
        int outer_thresh;
        int inner_thresh;
        int hev_thresh;
        GetDeblockFilterParams(level, &outer_thresh, &inner_thresh,
                               &hev_thresh);
        const dsp::LoopFilterSize size =
            GetLoopFilterSize(plane, filter_length);
        const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
        filter_func(src_row, src_stride, outer_thresh, inner_thresh,
                    hev_thresh);
      }
      // TODO(chengchen): use shifts instead of multiplication.
      src_row += row_step * src_stride;
      row_step = DivideBy4(row_step << subsampling_y);
    }
  }
}

void PostFilter::VerticalDeblockFilterNoMask(Plane plane, int row4x4_start,
                                             int column4x4_start, int unit_id) {
  static_cast<void>(unit_id);
  const int8_t subsampling_x = (plane == kPlaneY) ? 0 : subsampling_x_;
  const int8_t subsampling_y = (plane == kPlaneY) ? 0 : subsampling_y_;
  const int row_step = 1 << subsampling_y;
  const ptrdiff_t row_stride = MultiplyBy4(source_buffer_->stride(plane));
  const ptrdiff_t src_stride = source_buffer_->stride(plane);
  uint8_t* src = SetBufferOffset(source_buffer_, plane, row4x4_start,
                                 column4x4_start, subsampling_x, subsampling_y);
  const LoopFilterType type = kLoopFilterTypeVertical;
  int column_step;
  uint8_t level;
  int filter_length;

  for (int row4x4 = 0; MultiplyBy4(row4x4_start + row4x4) < height_ &&
                       row4x4 < kNum4x4InLoopFilterMaskUnit;
       row4x4 += row_step, src += row_stride) {
    uint8_t* src_row = src;
    for (int column4x4 = 0; MultiplyBy4(column4x4_start + column4x4) < width_ &&
                            column4x4 < kNum4x4InLoopFilterMaskUnit;
         column4x4 += column_step) {
      const bool need_filter = GetDeblockFilterEdgeInfo(
          plane, row4x4_start + row4x4, column4x4_start + column4x4,
          subsampling_x, subsampling_y, type, &level, &column_step,
          &filter_length);
      if (need_filter) {
        int outer_thresh;
        int inner_thresh;
        int hev_thresh;
        GetDeblockFilterParams(level, &outer_thresh, &inner_thresh,
                               &hev_thresh);
        const dsp::LoopFilterSize size =
            GetLoopFilterSize(plane, filter_length);
        const dsp::LoopFilterFunc filter_func = dsp_.loop_filters[size][type];
        filter_func(src_row, src_stride, outer_thresh, inner_thresh,
                    hev_thresh);
      }
      src_row += column_step * pixel_size_;
      column_step = DivideBy4(column_step << subsampling_x);
    }
  }
}

bool PostFilter::GetDeblockFilterEdgeInfo(
    const Plane plane, int row4x4, int column4x4, const int8_t subsampling_x,
    const int8_t subsampling_y, const LoopFilterType type, uint8_t* level,
    int* step, int* filter_length) const {
  row4x4 = GetDeblockPosition(row4x4, subsampling_y);
  column4x4 = GetDeblockPosition(column4x4, subsampling_x);
  const BlockParameters* bp = block_parameters_.Find(row4x4, column4x4);
  const TransformSize transform_size =
      (plane == kPlaneY) ? inter_transform_sizes_[row4x4][column4x4]
                         : bp->uv_transform_size;
  *step = (type == kLoopFilterTypeHorizontal) ? kTransformHeight[transform_size]
                                              : kTransformWidth[transform_size];
  if ((type == kLoopFilterTypeHorizontal && row4x4 == subsampling_y) ||
      (type == kLoopFilterTypeVertical && column4x4 == subsampling_x)) {
    return false;
  }

  const int filter_id = kDeblockFilterLevelIndex[plane][type];
  const uint8_t level_this = bp->deblock_filter_level[filter_id];
  const int row4x4_prev = (type == kLoopFilterTypeHorizontal)
                              ? row4x4 - (1 << subsampling_y)
                              : row4x4;
  const int column4x4_prev = (type == kLoopFilterTypeHorizontal)
                                 ? column4x4
                                 : column4x4 - (1 << subsampling_x);
  assert(row4x4_prev >= 0 && column4x4_prev >= 0);
  const BlockParameters* bp_prev =
      block_parameters_.Find(row4x4_prev, column4x4_prev);
  const uint8_t level_prev = bp_prev->deblock_filter_level[filter_id];
  *level = level_this;
  if (level_this == 0) {
    if (level_prev == 0) return false;
    *level = level_prev;
  }

  const BlockSize size =
      kPlaneResidualSize[bp->size][subsampling_x][subsampling_y];
  const int prediction_masks = (type == kLoopFilterTypeHorizontal)
                                   ? kBlockHeightPixels[size] - 1
                                   : kBlockWidthPixels[size] - 1;
  const int pixel_position = MultiplyBy4((type == kLoopFilterTypeHorizontal)
                                             ? row4x4 >> subsampling_y
                                             : column4x4 >> subsampling_x);
  const bool is_border = (pixel_position & prediction_masks) == 0;
  const bool skip = bp->skip && bp->is_inter;
  const bool skip_prev = bp_prev->skip && bp_prev->is_inter;
  if (!skip || !skip_prev || is_border) {
    const TransformSize transform_size_prev =
        (plane == kPlaneY) ? inter_transform_sizes_[row4x4_prev][column4x4_prev]
                           : bp_prev->uv_transform_size;
    const int step_prev = (type == kLoopFilterTypeHorizontal)
                              ? kTransformHeight[transform_size_prev]
                              : kTransformWidth[transform_size_prev];
    *filter_length = std::min(*step, step_prev);
    return true;
  }
  return false;
}

void PostFilter::InitDeblockFilterParams() {
  const int8_t sharpness = frame_header_.loop_filter.sharpness;
  assert(0 <= sharpness && sharpness < 8);
  const int shift = DivideBy4(sharpness + 3);  // ceil(sharpness / 4.0)
  for (int level = 0; level <= kMaxLoopFilterValue; ++level) {
    uint8_t limit = level >> shift;
    if (sharpness > 0) {
      limit = Clip3(limit, 1, 9 - sharpness);
    } else {
      limit = std::max(limit, static_cast<uint8_t>(1));
    }
    inner_thresh_[level] = limit;
    outer_thresh_[level] = 2 * (level + 2) + limit;
    hev_thresh_[level] = level >> 4;
  }
}

void PostFilter::GetDeblockFilterParams(uint8_t level, int* outer_thresh,
                                        int* inner_thresh,
                                        int* hev_thresh) const {
  *outer_thresh = outer_thresh_[level];
  *inner_thresh = inner_thresh_[level];
  *hev_thresh = hev_thresh_[level];
}

}  // namespace libgav1