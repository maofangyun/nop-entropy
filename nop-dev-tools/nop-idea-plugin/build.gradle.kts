plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"

    // for idea 2025+
    id("org.jetbrains.intellij.platform") version "2.1.0"
}



group = "io.github.entropy-cloud"
version = "1.0-SNAPSHOT"


repositories {
    mavenLocal()
    mavenCentral()

    // for idea 2025+
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts() // 这是使用本地环境所必须声明的仓库
    }
}



dependencies {
    // for idea 2025+
    intellijPlatform {
        // 由于解压后的 ZIP 包里缺少部分依赖映射，强行使用 local 会触发找不到依赖的错误。
        // 请在此处通过下载方式引入官方源：
        intellijIdeaCommunity("2025.2.2")

        bundledPlugins("com.intellij.java", "com.intellij.gradle", "org.jetbrains.plugins.yaml")
        
        pluginVerifier()
        zipSigner()
    }

    // ANTLR 适配器：https://github.com/antlr/antlr4-intellij-adaptor
    implementation("org.antlr:antlr4-intellij-adaptor:0.1")

    implementation("io.github.entropy-cloud:nop-markdown-ext:2.0.0-SNAPSHOT")
    implementation("io.github.entropy-cloud:nop-markdown:2.0.0-SNAPSHOT")
    implementation("io.github.entropy-cloud:nop-xlang-debugger:2.0.0-SNAPSHOT")
    implementation("io.github.entropy-cloud:nop-xlang:2.0.0-SNAPSHOT") {
        //exclude antlr4's dependency icu4j since it is not necessary and is too large.
        exclude(group = "com.ibm.icu")
    }

    testImplementation("junit:junit:4.13.2")
}

tasks {

    compileJava {
        options.encoding = "UTF-8"
    }

    compileTestJava {
        options.encoding = "UTF-8"
    }

    javadoc {
        options.encoding = "UTF-8"
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("261")
        untilBuild.set("263.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
