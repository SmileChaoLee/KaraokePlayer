# Install script for directory: /Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/src/main/jni/cpu_features/ndk_compat

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
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
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
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/release/arm64-v8a/cpu_features/ndk_compat/libndk_compat.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/ndk_compat" TYPE FILE FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/src/main/jni/cpu_features/ndk_compat/cpu-features.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xDevelx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets.cmake")
    file(DIFFERENT EXPORT_FILE_CHANGED FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets.cmake"
         "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/release/arm64-v8a/cpu_features/ndk_compat/CMakeFiles/Export/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets.cmake")
    if(EXPORT_FILE_CHANGED)
      file(GLOB OLD_CONFIG_FILES "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets-*.cmake")
      if(OLD_CONFIG_FILES)
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets.cmake\" will be replaced.  Removing files [${OLD_CONFIG_FILES}].")
        file(REMOVE ${OLD_CONFIG_FILES})
      endif()
    endif()
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat" TYPE FILE FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/release/arm64-v8a/cpu_features/ndk_compat/CMakeFiles/Export/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets.cmake")
  if("${CMAKE_INSTALL_CONFIG_NAME}" MATCHES "^([Rr][Ee][Ll][Ee][Aa][Ss][Ee])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat" TYPE FILE FILES "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/release/arm64-v8a/cpu_features/ndk_compat/CMakeFiles/Export/lib/cmake/CpuFeaturesNdkCompat/CpuFeaturesNdkCompatTargets-release.cmake")
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xDevelx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/CpuFeaturesNdkCompat" TYPE FILE FILES
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/release/arm64-v8a/cpu_features/CpuFeaturesNdkCompatConfig.cmake"
    "/Users/chaolee/AndroidStudio/KaraokePlayer/extensions/av1/.cxx/cmake/release/arm64-v8a/cpu_features/CpuFeaturesNdkCompatConfigVersion.cmake"
    )
endif()

