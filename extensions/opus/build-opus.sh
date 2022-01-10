OPUS_MODULE_PATH="$(pwd)/src/main"

# Download the [Android NDK][] and set its location in an environment variable:
# This build configuration has been tested on NDK r21.
NDK_PATH="/home/chaolee/Android/Sdk/ndk/22.1.7171670"

# Fetch libopus:
cd "${OPUS_MODULE_PATH}/jni" && \
git clone https://gitlab.xiph.org/xiph/opus.git libopus

# Run the script to convert arm assembly to NDK compatible format:
cd ${OPUS_MODULE_PATH}/jni && ./convert_android_asm.sh

# Build the JNI native libraries from the command line:
echo "Starting to build using ndk-build -j4"

cd "${OPUS_MODULE_PATH}"/jni
#${NDK_PATH}/ndk-build APP_ABI=all -j4
${NDK_PATH}/ndk-build -j4  # This one depends on Application.mk settings
# or
#${NDK_PATH}/ndk-build APP_ABI="x86 x86_64 armeabi-v7a arm64-v8a" -j4

echo "Finished to build using ndk-build -j4"