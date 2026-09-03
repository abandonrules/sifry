# Requires 3.13+ for target_link_options(); Gradle builds this with CMake 3.22.1.
cmake_minimum_required(VERSION 3.13)

project(SPA2)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library( regrep
             SHARED
             regrep.cpp )

file(GLOB pcre_sources "pcre-mini/*.c")

include_directories(
            .
            pcre-mini/
            pcre-mini/sljit/ )

add_definitions(-DHAVE_CONFIG_H -DPCRE_STATIC -DSLJIT_CONFIG_AUTO)

add_library( pcre-mini
             STATIC
             ${pcre_sources}
             pcre-mini/sljit/sljitLir.c )

find_library(android-lib android)

target_link_libraries( regrep
                       pcre-mini
                       ${android-lib} )

# 16 KB page-size compatibility (Android 15+ / API 35+ devices)
# Align LOAD segments to 16384 so the linker produces a 0x4000-aligned ELF,
# avoiding the "Android App Compatibility" dialog on 16 KB-page devices.
target_link_options( regrep PRIVATE
                     -Wl,-z,max-page-size=16384
                     -Wl,-z,common-page-size=16384 )
