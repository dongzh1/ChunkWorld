import org.jetbrains.kotlin.backend.common.phaser.transform

plugins {
    java
    id("com.github.johnrengelman.shadow") version ("8.1.1")
    id("com.xbaimiao.easylib") version ("1.1.6")
    kotlin("jvm") version "1.9.20"
}

group = "com.dongzh1.chunkworld"
version = "1.0.0"

easylib {
    env {
        mainClassName = "com.dongzh1.chunkworld.ChunkWorld"
        pluginName = "ChunkWorld"
        pluginUpdateInfo = "更新消息"
        kotlinVersion = "1.9.20"
    }
    version = "3.7.5"

    library("com.github.cryptomorin:XSeries:9.9.0", true) {
        relocate("com.cryptomorin.xseries", "${project.group}.shadow.xseries")
    }
//    library("de.tr7zw:item-nbt-api:2.12.3", true){
//        relocate("de.tr7zw.changeme.nbtapi", "${project.group}.shadow.itemnbtapi")
//        repo("https://repo.codemc.org/repository/maven-public/")
//    }
    library("redis.clients:jedis:5.0.1", true) {
        relocate("redis.clients.jedis", "${project.group}.shadow.redis")
    }
    // jedis需要
    library("org.apache.commons:commons-pool2:2.12.0", true){
        relocate("org.apache.commons.pool2", "${project.group}.shadow.pool2")
    }
    library("com.zaxxer:HikariCP:4.0.3", true) {
        relocate("com.zaxxer.hikari", "${project.group}.shadow.hikari")
    }
    library("com.j256.ormlite:ormlite-core:5.6", true) {
        relocate("com.j256.ormlite", "${project.group}.shadow.ormlite")
    }
    library("com.j256.ormlite:ormlite-jdbc:5.6", true) {
        relocate("com.j256.ormlite", "${project.group}.shadow.ormlite")
    }

    relocate("com.xbaimiao.easylib", "${project.group}.easylib", false)
    relocate("kotlin", "${project.group}.shadow.kotlin", true)
    relocate("kotlinx", "${project.group}.shadow.kotlinx", true)
}

repositories {
    mavenLocal()
    mavenCentral()
    // auto-inject
    easylib.library.mapNotNull { it.repo }.toSet().forEach { uri -> maven(uri) }

    maven("https://maven.xbaimiao.com/repository/maven-public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    easylib.library.forEach {
        if (it.cloud) {
            compileOnly(it.id)
        } else {
            implementation(it.id)
        }
    }

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    //compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly(fileTree("libs"))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    shadowJar {
        dependencies {
            easylib.library.forEach {
                if (it.cloud) {
                    exclude(dependency(it.id))
                }
            }
            exclude(dependency("org.slf4j:"))
            exclude(dependency("org.jetbrains:annotations:"))
            exclude(dependency("com.google.code.gson:gson:"))
            exclude(dependency("org.jetbrains.kotlin:"))
            exclude(dependency("org.jetbrains.kotlinx:"))
        }
        archiveClassifier.set("")
        easylib.getAllRelocate().forEach {
            relocate(it.pattern, it.replacement)
        }
        minimize()
    }
    processResources {
        expand("version" to project.version)
        val relocateAnchor = "relocate: # inject"
        filteringCharset = "UTF-8"
        // 添加过滤器以排除 .dat 文件
        //exclude("**/world/level.dat", "**/world/session.lock", "**/world/region/*.mca")
        from("src/main/resources") {
            include("world/**")
            into("world")
            filteringCharset = "UTF-8"
        }
        filter { line ->
            var replace = line
            if (line.contains(relocateAnchor)) {
                replace = line.replace(relocateAnchor,
                    "relocate: \r\n" + easylib.getAllRelocate().filter { it.cloud }
                        .joinToString("\r\n") { "  - \"${it.pattern}!${it.replacement}\"" })
            }
            if (line.contains("main: # inject")) {
                replace = "main: ${easylib.env.mainClassName}"
            }
            if (line.contains("name: # inject")) {
                replace = "name: ${easylib.env.pluginName}"
            }
            if (line.contains("update-info: # inject")) {
                replace = "update-info: \"${easylib.env.pluginUpdateInfo}\""
            }
            if (line.contains("kotlin-version: # inject")) {
                replace = "kotlin-version: \"${easylib.env.kotlinVersion}\""
            }
            if (line.contains("depend-list: # inject")) {
                replace = "depend-list: \r\n" + easylib.library.filter { it.cloud }.joinToString("\r\n") {
                    if (it.repo == null) {
                        "  - \"${it.id}\""
                    } else {
                        "  - \"${it.id}<<repo>>${it.repo}\""
                    }
                }
            }
            replace
        }
    }
}
