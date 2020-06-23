#cd /Users/chaolee/AndroidStudio/ExoPlayerDemo
#EXOPLAYER_ROOT="$(pwd)"
#echo ${EXOPLAYER_ROOT}

#FFMPEG_EXT_PATH="${EXOPLAYER_ROOT}/extensions/ffmpeg/src/main"
FFMPEG_EXT_PATH="$(pwd)/src/main"
echo ${FFMPEG_EXT_PATH}

# NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r15c"
NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r20b"
echo ${NDK_PATH}

HOST_PLATFORM="darwin-x86_64"
echo ${HOST_PLATFORM}

TOOLCHAIN_PREFIX="${NDK_PATH}/toolchains/llvm/prebuilt/${HOST_PLATFORM}/bin"
echo ${TOOLCHAIN_PREFIX}

COMMON_OPTIONS="\
    --target-os=android
    --disable-static
    --enable-shared
    --disable-doc
    --disable-programs
    --disable-everything
    --disable-avdevice
    --disable-avformat
    --disable-swscale
    --disable-postproc
    --disable-avfilter
    --disable-symver
    --enable-avresample
    --enable-swresample
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
cd ffmpeg && git checkout release/4.2 && \
./configure \
    --libdir=android-libs/armeabi-v7a \
    --arch=arm \
    --cpu=armv7-a \
    --cross-prefix="${TOOLCHAIN_PREFIX}/armv7a-linux-androideabi16-" \
    --nm="${TOOLCHAIN_PREFIX}/arm-linux-androideabi-nm" \
    --strip="${TOOLCHAIN_PREFIX}/arm-linux-androideabi-strip" \
    --extra-cflags="-march=armv7-a -mfloat-abi=softfp" \
    --extra-ldflags="-Wl,--fix-cortex-a8" \
    --extra-ldexeflags=-pie \
    ${COMMON_OPTIONS}
make -j4
make install-libs
make clean
./configure \
    --libdir=android-libs/arm64-v8a \
    --arch=aarch64 \
    --cpu=armv8-a \
    --cross-prefix="${TOOLCHAIN_PREFIX}/aarch64-linux-android21-" \
    --nm="${TOOLCHAIN_PREFIX}/aarch64-linux-android-nm" \
    --strip="${TOOLCHAIN_PREFIX}/aarch64-linux-android-strip" \
    --extra-ldexeflags=-pie \
    ${COMMON_OPTIONS}
make -j4
make install-libs
make clean
./configure \
    --libdir=android-libs/x86 \
    --arch=x86 \
    --cpu=i686 \
    --cross-prefix="${TOOLCHAIN_PREFIX}/i686-linux-android16-" \
    --nm="${TOOLCHAIN_PREFIX}/i686-linux-android-nm" \
    --strip="${TOOLCHAIN_PREFIX}/i686-linux-android-strip" \
    --extra-ldexeflags=-pie \
    --disable-asm \
    ${COMMON_OPTIONS}
make -j4
make install-libs
make clean
./configure \
    --libdir=android-libs/x86_64 \
    --arch=x86_64 \
    --cpu=x86_64 \
    --cross-prefix="${TOOLCHAIN_PREFIX}/x86_64-linux-android21-" \
    --nm="${TOOLCHAIN_PREFIX}/x86_64-linux-android-nm" \
    --strip="${TOOLCHAIN_PREFIX}/x86_64-linux-android-strip" \
    --extra-ldexeflags=-pie \
    --disable-asm \
    ${COMMON_OPTIONS} \
    && \
make -j4
make install-libs
make clean
echo "make clean --> finished"
echo " "

cd "${FFMPEG_EXT_PATH}"/jni && \
${NDK_PATH}/ndk-build APP_ABI="armeabi-v7a arm64-v8a x86 x86_64" -j4
