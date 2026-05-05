cmake_minimum_required(VERSION 3.22.1)

project("lwjgl2")

if (CMAKE_SIZEOF_VOID_P EQUAL 8)
   set(LIBNAME lwjgl64)
else()
   set(LIBNAME lwjgl)
endif()

find_package(Threads REQUIRED)
find_package(JNI REQUIRED)
find_package(glfw3 REQUIRED)

if(WIN32)
   set(PLATFORM "windows")
elseif(UNIX AND APPLE)
   set(PLATFORM "macosx")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
   set(PLATFORM "linux")
endif()

get_filename_component(NATIVE "../src/native" REALPATH)

file(GLOB_RECURSE GENERATED_AL_SRC ${NATIVE}/generated/openal/*.c)
file(GLOB_RECURSE GENERATED_GL_SRC ${NATIVE}/generated/opencl/*.c)
file(GLOB_RECURSE GENERATED_CL_SRC ${NATIVE}/generated/opengl/*.c)
file(GLOB COMMON_SRC       ${NATIVE}/common/*.c)
file(GLOB COMMON_GL_SRC    ${NATIVE}/common/opengl/*.c)

file(GLOB PLATFORM_SRC        ${NATIVE}/${PLATFORM}/*.c)

set(GLFWBASE ${NATIVE}/common/glfw/)

set(COMMON_GLFW_SRC
        ${GLFWBASE}/org_lwjgl_opengl_GLFWPeerInfo.c
        ${GLFWBASE}/org_lwjgl_opengl_GLFWContextImplementation.c
        ${GLFWBASE}/org_lwjgl_opengl_GLFWDisplay.c
        ${GLFWBASE}/glfw_hints.c
        ${GLFWBASE}/extgl_glfw.c
)

list(APPEND LWJGL_SRC ${GENERATED_AL_SRC} ${GENERATED_GL_SRC} ${GENERATED_CL_SRC} ${COMMON_SRC} ${COMMON_GL_SRC} ${COMMON_GLFW_SRC} ${PLATFORM_SRC})

add_library(${LIBNAME} SHARED ${LWJGL_SRC})

target_include_directories(${LIBNAME} PRIVATE ${JNI_INCLUDE_DIRS} ${NATIVE}/common ${NATIVE}/common/opengl ${NATIVE}/${PLATFORM})
if(PLATFORM STREQUAL "macosx")
   target_link_libraries(${LIBNAME} glfw m Threads::Threads "-framework CoreFoundation" ${JAVA_AWT_LIBRARY})
else ()
   target_link_libraries(${LIBNAME} glfw m Threads::Threads)
endif ()
target_link_libraries(${LIBNAME} glfw m Threads::Threads)
target_compile_options(${LIBNAME} PRIVATE -O2 -Wall)
set_property(TARGET ${LIBNAME} PROPERTY C_STANDARD 99)
set_property(TARGET ${LIBNAME} PROPERTY C_STANDARD_REQUIRED ON)

if(PLATFORM STREQUAL "linux")
   target_link_options(${LIBNAME} PRIVATE "-Wl,--version-script=${NATIVE}/linux/lwjgl.map")
endif ()
