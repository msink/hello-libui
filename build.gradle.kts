plugins {
    id("kotlin-multiplatform") version "1.3.20-eap-100"
}

repositories {
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven { url = uri("https://dl.bintray.com/msink/kotlin-dev") }
    jcenter()
}

val os = org.gradle.internal.os.OperatingSystem.current()!!

val resourcesDir = "$projectDir/src/nativeMain/resources"
val windowsResources = "$buildDir/resources/hello.res"

val compileWindowsResources by tasks.registering(Exec::class) {
    onlyIf { os.isWindows }

    val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
    val konanLlvmDir = "$konanUserDir/dependencies/msys2-mingw-w64-x86_64-gcc-7.3.0-clang-llvm-lld-6.0.1/bin"
    val rcFile = file("$resourcesDir/hello.rc")

    inputs.file(rcFile)
    outputs.file(file(windowsResources))
    commandLine("cmd", "/c", "windres", rcFile, "-O", "coff", "-o", windowsResources)
    environment("PATH", "c:/msys64/mingw64/bin;$konanLlvmDir;${System.getenv("PATH")}")
}

kotlin {
    sourceSets.create("nativeMain") {
        dependencies {
            implementation("com.github.msink:libui:0.2.0-dev")
        }
    }
    val nativeTarget = when {
        os.isWindows -> mingwX64("native")
        os.isMacOsX -> macosX64("native")
        os.isLinux -> linuxX64("native")
        else -> throw Error("Unknown host")
    }
    configure(listOf(nativeTarget)) {
        binaries {
            executable(listOf(DEBUG)) {
                if (os.isWindows) {
                    tasks.named("compileKotlinNative") { dependsOn(compileWindowsResources) }
                    linkerOpts("$windowsResources -mwindows")
                }
            }
        }
    }
}
