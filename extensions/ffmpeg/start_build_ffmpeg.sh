#!/bin/bash

./download_ffmpeg.sh && \

FFMPEG_EXT_PATH="$(pwd)/src/main"
echo ${FFMPEG_EXT_PATH}
NDK_PATH="/home/chaolee/Android/Sdk/ndk/21.4.7075529"
echo ${NDK_PATH}
HOST_PLATFORM="linux-x86_64"
echo ${HOST_PLATFORM}
ENABLED_DECODERS=(vorbis opus flac)

cd "${FFMPEG_EXT_PATH}/jni" && \
./build_ffmpeg.sh \
  "${FFMPEG_EXT_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ENABLED_DECODERS[@]}"

echo "make clean --> finished"
echo " "
