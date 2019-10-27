#cd /Users/chaolee/AndroidStudio/ExoPlayerDemo
#EXOPLAYER_ROOT="$(pwd)"
#echo ${EXOPLAYER_ROOT}

#FFMPEG_EXT_PATH="${EXOPLAYER_ROOT}/extensions/ffmpeg/src/main"
FFMPEG_EXT_PATH="$(pwd)/src/main"
echo ${FFMPEG_EXT_PATH}

NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r15c"
echo ${NDK_PATH}

HOST_PLATFORM="darwin-x86_64"
echo ${HOST_PLATFORM}

COMMON_OPTIONS="\
    --target-os=android \
    --disable-static \
    --enable-shared \
    --disable-doc \
    --disable-programs \
    --disable-everything \
    --disable-avdevice \
    --disable-avformat \
    --disable-swscale \
    --disable-postproc \
    --disable-avfilter \
    --disable-symver \
    --disable-swresample \
    --enable-avresample \
    --enable-decoder=vorbis \
    --enable-decoder=opus \
    --enable-decoder=flac \
    --enable-decoder=alac \
    --enable-decoder=pcm_mulaw \
    --enable-decoder=pcm_alaw \
    --enable-decoder=mp3 \
    --enable-decoder=amrnb \
    --enable-decoder=amrwb \
    --enable-decoder=aac \
    --enable-decoder=ac3 \
    --enable-decoder=eac3 \
    --enable-decoder=dca \
    --enable-decoder=mlp \
    --enable-decoder=truehd \
    " && \
cd "${FFMPEG_EXT_PATH}/jni" && \
(git -C ffmpeg pull || git clone git://source.ffmpeg.org/ffmpeg ffmpeg) && \
cd ffmpeg && git checkout release/4.0 && \
./configure \
    --libdir=android-libs/armeabi-v7a \
    --arch=arm \
    --cpu=armv7-a \
    --cross-prefix="${NDK_PATH}/toolchains/arm-linux-androideabi-4.9/prebuilt/${HOST_PLATFORM}/bin/arm-linux-androideabi-" \
    --sysroot="${NDK_PATH}/platforms/android-9/arch-arm/" \
    --extra-cflags="-march=armv7-a -mfloat-abi=softfp" \
    --extra-ldflags="-Wl,--fix-cortex-a8" \
    --extra-ldexeflags=-pie \
    ${COMMON_OPTIONS} \
    && \
make -j4 && make install-libs && \
make clean && ./configure \
    --libdir=android-libs/arm64-v8a \
    --arch=aarch64 \
    --cpu=armv8-a \
    --cross-prefix="${NDK_PATH}/toolchains/aarch64-linux-android-4.9/prebuilt/${HOST_PLATFORM}/bin/aarch64-linux-android-" \
    --sysroot="${NDK_PATH}/platforms/android-21/arch-arm64/" \
    --extra-ldexeflags=-pie \
    ${COMMON_OPTIONS} \
    && \
make -j4 && make install-libs && \
make clean && ./configure \
    --libdir=android-libs/x86 \
    --arch=x86 \
    --cpu=i686 \
    --cross-prefix="${NDK_PATH}/toolchains/x86-4.9/prebuilt/${HOST_PLATFORM}/bin/i686-linux-android-" \
    --sysroot="${NDK_PATH}/platforms/android-9/arch-x86/" \
    --extra-ldexeflags=-pie \
    --disable-asm \
    ${COMMON_OPTIONS} \
    && \
make -j4 && make install-libs && \
make clean && ./configure \
    --libdir=android-libs/x86_64 \
    --arch=x86_64 \
    --cpu=x86_64 \
    --cross-prefix="${NDK_PATH}/toolchains/x86_64-4.9/prebuilt/${HOST_PLATFORM}/bin/x86_64-linux-android-" \
    --sysroot="${NDK_PATH}/platforms/android-21/arch-x86_64/" \
    --extra-ldexeflags=-pie \
    --disable-asm \
    ${COMMON_OPTIONS} \
    && \
make -j4 && make install-libs && \
make clean
echo "make clean --> finished"
echo " "

cd "${FFMPEG_EXT_PATH}"/jni && \
${NDK_PATH}/ndk-build APP_ABI="armeabi-v7a arm64-v8a x86 x86_64" -j4
