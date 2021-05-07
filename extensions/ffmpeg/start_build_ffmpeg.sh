#!/bin/bash

./download_ffmpeg.sh && \

FFMPEG_EXT_PATH="$(pwd)/src/main"
echo ${FFMPEG_EXT_PATH}
NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r20b"
echo ${NDK_PATH}
HOST_PLATFORM="darwin-x86_64"
echo ${HOST_PLATFORM}
ENABLED_DECODERS=(vorbis opus flac)

cd "${FFMPEG_EXT_PATH}/jni" && \
./build_ffmpeg.sh \
  "${FFMPEG_EXT_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ENABLED_DECODERS[@]}"

echo "make clean --> finished"
echo " "