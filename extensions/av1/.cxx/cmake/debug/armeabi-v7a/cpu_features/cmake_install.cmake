# Install script for directory: /Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/src/main/jni/cpu_features

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/libcpu_features.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/cpu_features" TYPE FILE FILES
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/src/main/jni/cpu_features/include/cpu_features_macros.h"
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/src/main/jni/cpu_features/include/cpu_features_cache_info.h"
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/src/main/jni/cpu_features/include/cpuinfo_arm.h"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/bin" TYPE EXECUTABLE FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/list_cpu_features")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/list_cpu_features" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/list_cpu_features")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/Users/chaolee/Library/Android/sdk/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/list_cpu_features")
    endif()
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xDevelx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures/CpuFeaturesTargets.cmake")
    file(DIFFERENT EXPORT_FILE_CHANGED FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures/CpuFeaturesTargets.cmake"
         "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/CMakeFiles/Export/lib/cmake/CpuFeatures/CpuFeaturesTargets.cmake")
    if(EXPORT_FILE_CHANGED)
      file(GLOB OLD_CONFIG_FILES "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures/CpuFeaturesTargets-*.cmake")
      if(OLD_CONFIG_FILES)
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures/CpuFeaturesTargets.cmake\" will be replaced.  Removing files [${OLD_CONFIG_FILES}].")
        file(REMOVE ${OLD_CONFIG_FILES})
      endif()
    endif()
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures" TYPE FILE FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/CMakeFiles/Export/lib/cmake/CpuFeatures/CpuFeaturesTargets.cmake")
  if("${CMAKE_INSTALL_CONFIG_NAME}" MATCHES "^([Dd][Ee][Bb][Uu][Gg])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures" TYPE FILE FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/CMakeFiles/Export/lib/cmake/CpuFeatures/CpuFeaturesTargets-debug.cmake")
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xDevelx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeatures" TYPE FILE FILES
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/CpuFeaturesConfig.cmake"
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/CpuFeaturesConfigVersion.cmake"
    )
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  include("/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/debug/armeabi-v7a/cpu_features/ndk_compat/cmake_install.cmake")

endif()

