plugins {
    kotlin("multiplatform") version "1.3.20"
}

repositories {
    jcenter()
}

kotlin {
    sourceSets.create("nativeMain") {
        dependencies {
            implementation("com.github.msink:libui:0.1.2")
        }
    }

    val os = org.gradle.internal.os.OperatingSystem.current()!!
    when {
        os.isWindows -> mingwX64("native")
        os.isMacOsX -> macosX64("native")
        os.isLinux -> linuxX64("native")
        else -> throw Error("Unknown host")
    }.apply {
        binaries {
            executable(listOf(DEBUG)) {
                if (os.isWindows) {
                    windowsResources("hello.rc")
                    linkerOpts("-mwindows")
                }
            }
        }
    }
}

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val rcFile = file("${compilation.defaultSourceSet.resources.sourceDirectories.asPath}/$rcFileName")
    val resFile = file("$buildDir/windowsResources/${target.getName()}/${buildType.getName()}/${rcFileName.substringBefore(".rc")}.res")

    val windresTask = tasks.create<Exec>("compileWindowsResources${buildType.getName().capitalize()}${target.getName().capitalize()}") {
        val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
        val konanLlvmDir = "$konanUserDir/dependencies/msys2-mingw-w64-x86_64-gcc-7.3.0-clang-llvm-lld-6.0.1/bin"

        inputs.file(rcFile)
        outputs.file(resFile)
        commandLine("$konanLlvmDir/windres", rcFile, "-D_${buildType.name}", "-O", "coff", "-o", resFile)
        environment("PATH", "$konanLlvmDir;${System.getenv("PATH")}")

        dependsOn(compilation.compileKotlinTask)
    }

    linkTask.dependsOn(windresTask)
    linkerOpts(resFile.toString())
}
