cmake_minimum_required(VERSION 3.4.1)

add_library( regrep
             SHARED
             src/main/jni/regrep.c )

file(GLOB pcre_sources "src/main/jni/pcre-mini/*.c")

include_directories(
            src/main/jni/
            src/main/jni/pcre-mini/
            src/main/jni/pcre-mini/sljit/ )

add_definitions(-DHAVE_CONFIG_H -DPCRE_STATIC -DSLJIT_CONFIG_AUTO)

add_library( pcre-mini
             STATIC
             ${pcre_sources}
             "src/main/jni/pcre-mini/sljit/sljitLir.c" )

find_library(android-lib android)

target_link_libraries( regrep
                       pcre-mini
                       ${android-lib} )