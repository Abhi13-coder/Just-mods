cmake_minimum_required(VERSION 3.10)
project(toonsky)

set(CMAKE_CXX_STANDARD 17)

add_library(toonsky SHARED src/main.cpp)

target_include_directories(toonsky PRIVATE include)

target_link_libraries(toonsky
    android
    log
    GLESv2
    EGL
)

target_compile_options(toonsky PRIVATE
    -O2
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
)

target_link_options(toonsky PRIVATE
    -Wl,--gc-sections
    -Wl,--strip-all
)
