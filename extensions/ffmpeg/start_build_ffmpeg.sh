#!/bin/bash

./download_ffmpeg.sh && \

FFMPEG_EXT_PATH="$(pwd)/src/main"
echo ${FFMPEG_EXT_PATH}
NDK_PATH="/home/chaolee/Android/Sdk/ndk/22.1.7171670"
echo ${NDK_PATH}
HOST_PLATFORM="linux-x86_64"
echo ${HOST_PLATFORM}
ENABLED_DECODERS=(vorbis opus flac alac pcm_mulaw pcm_alaw aac mp3 amrnb amrwb aac ac3 eac3 dca mlp truehd)

cd "${FFMPEG_EXT_PATH}/jni" && \
./build_ffmpeg.sh \
  "${FFMPEG_EXT_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ENABLED_DECODERS[@]}"

echo "make clean --> finished"
echo " "
