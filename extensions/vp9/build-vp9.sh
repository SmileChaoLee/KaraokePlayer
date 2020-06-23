#cd "<path to exoplayer checkout>"
#EXOPLAYER_ROOT="$(pwd)"
#VP9_EXT_PATH="${EXOPLAYER_ROOT}/extensions/vp9/src/main"

#This script file must be under ${EXOPLAYER_ROOT}/extensions/vp9"
VP9_EXT_PATH="$(pwd)/src/main"

# Download the [Android NDK][] and set its location in an environment variable.
# NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r15c"  # this works fine
NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r20b"

#Fetch libvpx:
cd "${VP9_EXT_PATH}/jni"
git clone https://chromium.googlesource.com/webm/libvpx libvpx

# Checkout the appropriate branch of libvpx (the scripts and makefiles bundled
#  in this repo are known to work only at specific versions of the library - we
#  will update this periodically as newer versions of libvpx are released):
cd "${VP9_EXT_PATH}/jni/libvpx"
git checkout tags/v1.8.0 -b v1.8.0

# Run a script that generates necessary configuration files for libvpx:
cd ${VP9_EXT_PATH}/jni
./generate_libvpx_android_configs.sh

# Build the JNI native libraries from the command line:
echo "Starting to build using ndk-build -j4"

cd "${VP9_EXT_PATH}"/jni
#${NDK_PATH}/ndk-build APP_ABI=all -j4
${NDK_PATH}/ndk-build -j4  # This one depends on Application.mk settings
# or
#${NDK_PATH}/ndk-build APP_ABI="x86 x86_64 armeabi-v7a arm64-v8a" -j4

echo "Finished to build using ndk-build -j4"