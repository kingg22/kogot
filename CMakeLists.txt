cmake_minimum_required(VERSION 3.22)
project(godot-java-extension C)

set(CMAKE_C_STANDARD 17)

# Find Java and JNI
find_package(JNI REQUIRED)
include_directories(${PROJECT_NAME}
    ${JNI_INCLUDE_DIRS}
)

# Platform-specific settings
if(WIN32)
    set(TARGET_PATH "bin/libgdjava.windows")
    set(LIB_EXTENSION "dll")
elseif(APPLE)
    set(TARGET_PATH "bin/libgdjava.macos")
    set(LIB_EXTENSION "dylib")
else()
    set(TARGET_PATH "bin/libgdjava.linux")
    set(LIB_EXTENSION "so")
endif()

# Build type suffix
if(CMAKE_BUILD_TYPE MATCHES Debug)
    set(BUILD_SUFFIX "debug")
else()
    set(BUILD_SUFFIX "release")
endif()

# Architecture detection
if(CMAKE_SIZEOF_VOID_P EQUAL 8)
    set(ARCH_SUFFIX "x86_64")
else()
    set(ARCH_SUFFIX "x86_32")
endif()

set(OUTPUT_NAME "${TARGET_PATH}.${BUILD_SUFFIX}.${ARCH_SUFFIX}")

# Source files
set(SOURCES
    godot-extension-register/gdextension_interface.h
    godot-extension-register/gdextension_init.h
    godot-extension-register/gdextension_init.c
    # Add more source files here
)

# Create shared library
add_library(${PROJECT_NAME} SHARED ${SOURCES})

# Link libraries
target_link_libraries(${PROJECT_NAME}
    ${JNI_LIBRARIES}
)

# Platform-specific link options
if(UNIX AND NOT APPLE)
    target_link_libraries(${PROJECT_NAME} dl)
endif()

# Set output properties
set_target_properties(${PROJECT_NAME} PROPERTIES
    PREFIX ""
    OUTPUT_NAME "gdjava"
    SUFFIX ".${ARCH_SUFFIX}.${LIB_EXTENSION}"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/${TARGET_PATH}.${BUILD_SUFFIX}"
)

# Compiler flags
if(CMAKE_BUILD_TYPE MATCHES Debug)
    target_compile_definitions(${PROJECT_NAME} PRIVATE DEBUG_ENABLED)
    if(MSVC)
        target_compile_options(${PROJECT_NAME} PRIVATE /W4 /MDd)
    else()
        target_compile_options(${PROJECT_NAME} PRIVATE -g -Wall -Wextra)
    endif()
else()
    if(MSVC)
        target_compile_options(${PROJECT_NAME} PRIVATE /O2 /MD)
    else()
        target_compile_options(${PROJECT_NAME} PRIVATE -O3)
    endif()
endif()

# Install targets (optional)
install(TARGETS ${PROJECT_NAME}
    LIBRARY DESTINATION bin
    RUNTIME DESTINATION bin
)
