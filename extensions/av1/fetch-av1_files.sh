#cd "<path to exoplayer checkout>"
#EXOPLAYER_ROOT="$(pwd)"
#AV1_EXT_PATH="${EXOPLAYER_ROOT}/extensions/av1/src/main"

#This script file must be under ${EXOPLAYER_ROOT}/extensions/av1"
AV1_EXT_PATH="$(pwd)/src/main"

# Download the [Android NDK][] and set its location in an environment variable.
NDK_PATH="/Users/chaolee/Library/Android/sdk/android-ndk-r20b"

# Fetch cpu_features:
cd "${AV1_EXT_PATH}/jni"
git clone https://github.com/google/cpu_features

# Fetch libgav1:
cd "${AV1_EXT_PATH}/jni"
git clone https://chromium.googlesource.com/codecs/libgav1 libgav1

# Fetch Abseil:
cd "${AV1_EXT_PATH}/jni/libgav1"
mkdir "${AV1_EXT_PATH}/jni/libgav1/third_party"
cd "${AV1_EXT_PATH}/jni/libgav1/third_party"
#git clone https://github.com/abseil/abseil-cpp.git third_party/abseil-cpp
git clone https://github.com/abseil/abseil-cpp.git
