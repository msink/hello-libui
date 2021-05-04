plugins {
    kotlin("multiplatform") version "1.5.0"
}

repositories {
    jcenter()
}

val os = org.gradle.internal.os.OperatingSystem.current()!!

kotlin {
    when {
        //for 32-bit Windows - replace
        // here mingwX64 to mingwX86 and
        // below x86_64 to i686
        os.isWindows -> mingwX64("libui")
        os.isMacOsX -> macosX64("libui")
        os.isLinux -> linuxX64("libui")
        else -> throw Error("Unknown host")
    }.binaries.executable {
        if (os.isWindows) {
            windowsResources("hello.rc")
            linkerOpts("-mwindows")
        }
    }
    val libuiMain by sourceSets.getting {
        dependencies {
            implementation("com.github.msink:libui:0.1.8")
        }
    }
}

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.defaultSourceSet.resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = buildDir.resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
        val konanLlvmDir = "$konanUserDir/dependencies/msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1/bin"

        inputs.file(inFile)
        outputs.file(outFile)
        commandLine("$konanLlvmDir/windres", inFile, "-D_${buildType.name}", "-O", "coff", "-o", outFile)
        environment("PATH", "$konanLlvmDir;${System.getenv("PATH")}")

        dependsOn(compilation.compileKotlinTask)
    }

    linkTask.dependsOn(windresTask)
    linkerOpts(outFile.toString())
}
