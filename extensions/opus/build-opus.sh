#cd "<path to exoplayer checkout>"
#EXOPLAYER_ROOT="$(pwd)"
#OPUS_EXT_PATH="${EXOPLAYER_ROOT}/extensions/opus/src/main"

#This script file must be under ${EXOPLAYER_ROOT}/extensions/opus"
OPUS_EXT_PATH="$(pwd)/src/main"

# Download the [Android NDK][] and set its location in an environment variable:
# NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r15c"
NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r20b"

# Fetch libopus:
cd "${OPUS_EXT_PATH}/jni"
git clone https://git.xiph.org/opus.git libopus

# Run the script to convert arm assembly to NDK compatible format:
cd ${OPUS_EXT_PATH}/jni && ./convert_android_asm.sh

# Build the JNI native libraries from the command line:
echo "Starting to build using ndk-build -j4"

cd "${OPUS_EXT_PATH}"/jni
#${NDK_PATH}/ndk-build APP_ABI=all -j4
${NDK_PATH}/ndk-build -j4  # This one depends on Application.mk settings
# or
#${NDK_PATH}/ndk-build APP_ABI="x86 x86_64 armeabi-v7a arm64-v8a" -j4

echo "Finished to build using ndk-build -j4"