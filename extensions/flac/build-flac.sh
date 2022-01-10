#cd "<path to exoplayer checkout>"
#EXOPLAYER_ROOT="$(pwd)"
#FLAC_EXT_PATH="${EXOPLAYER_ROOT}/extensions/flac/src/main"

#This script file must be under ${EXOPLAYER_ROOT}/extensions/flac"
FLAC_EXT_PATH="$(pwd)/src/main"

# Download the [Android NDK][] and set its location in an environment variable.
# NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r15c"
# or
#NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r17c"  # this one works too

NDK_PATH="/home/chaolee/Android/Sdk/ndk/22.1.7171670"

# Download and extract flac-1.3.2 as "${FLAC_EXT_PATH}/jni/flac" folder:
cd "${FLAC_EXT_PATH}/jni"
curl https://ftp.osuosl.org/pub/xiph/releases/flac/flac-1.3.2.tar.xz | tar xJ && \
mv flac-1.3.2 flac

# Build the JNI native libraries from the command line:
echo "Starting to build using ndk-build -j4"

cd "${FLAC_EXT_PATH}"/jni
#${NDK_PATH}/ndk-build APP_ABI=all -j4
${NDK_PATH}/ndk-build -j4  # This one depends on Application.mk settings
# or
#${NDK_PATH}/ndk-build APP_ABI="x86 x86_64 armeabi-v7a arm64-v8a" -j4

echo "Finished to build using ndk-build -j4"