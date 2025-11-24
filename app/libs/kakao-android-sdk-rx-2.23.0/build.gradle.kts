/*
  Copyright 2019 Kakao Corp.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.gradle}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = java.net.URI("https://devrepo.kakao.com/nexus/content/groups/public/")
        }
    }
}

val updateTestData by tasks.registering(Exec::class) {
    group = "verification"

    val isOpenSdk = projectDir.absolutePath.contains("open")

    val directory = "kakao-sdk-test-data"

    val path = listOf(
        project.projectDir,
        if (isOpenSdk) "" else "open",
        "kakao-sdk-test-data"
    ).joinToString("/")

    val testDataDir = File(path)

    if (testDataDir.exists()) {
        workingDir = testDataDir

        commandLine("git", "fetch")
        commandLine("git", "pull", "origin", "master")

    } else {
        val cloneDir = testDataDir.absolutePath.removeSuffix(directory)
        workingDir = File(cloneDir)
        println(cloneDir)
        commandLine("git", "clone", "git@github.daumkakao.com:kops/kakao-sdk-test-data.git")
    }
}

val libraries = rootProject.subprojects.filter { project ->
    project.name !in Dokka.samples
}

configure(libraries) {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
        moduleName.set(Publish.projectName)
        dokkaSourceSets {
            configureEach {
                includes.from("../packages.md")
                skipEmptyPackages.set(true)

                noJdkLink = true
                noStdlibLink = true
                noAndroidSdkLink = true

                listOf("androidx", "dagger", "io.reactivex", "com.google").forEach {
                    perPackageOption {
                        matchingRegex.set("${it}($|\\.).*")
                        suppress.set(true)
                    }
                }
            }
        }
    }
}

plugins {
    id("org.jetbrains.dokka") version "1.8.10"
    id("com.github.ben-manes.versions") version "0.27.0"
    `maven-publish`
}

val clean by tasks.registering(Delete::class) {
    delete(rootProject.layout.buildDirectory.get().asFile)
}

val publishSdk by tasks.registering(Task::class) {
    group = "publishing"

    libraries.filter { it.name !in Publish.excludedLibraries }.forEach { project ->
        project.tasks.findByName("publish")?.let {
            dependsOn(it)
        }
    }
    tasks.findByName("publishProjectPublicationToMavenRepository")?.let {
        dependsOn(it)
    }
}

val fullSourcePath = "${rootProject.layout.buildDirectory.dir("full_source").get().asFile}"

val copyProject by tasks.registering(Copy::class) {
    from(rootProject.rootDir)
    into(fullSourcePath)
    exclude(
        relativePath(rootProject.layout.buildDirectory.get().asFile),
        "**/.gradle",
        ".idea",
        ".project.info",
        "**/jacoco.exec",
        "**/*.iml",
        "buildSrc/build",
        "README.md",
        "rxpartnersdkv2Ko.md",
        "*.keystore",
        "Dockerfile",
        "Jenkinsfile",
        "deploy.sh",
        "**/kakao-sdk-test-data",
    )
    exclude(rootProject.subprojects.map { relativePath(it.layout.buildDirectory.get().asFile) })

    // 샘플 프로젝트에서 sdk 모듈 제외
    rootProject.subprojects
        .filter { it.project.name !in Publish.samples }
        .forEach { exclude(relativePath(it.projectDir)) }
}

fun File.replaceDependencyToRemote(moduleName: String): Unit = writeText(
    readText().replace(
        "project(\":$moduleName\")",
        "\"com.kakao.sdk:v2-$moduleName:${SdkVersions.version}\""
    )
)

fun replaceSampleDependency(directory: File) {
    directory.listFiles()?.forEach { file ->
        val postFix = if (directory.name.contains("rx") ||
            directory.name.contains("java") ||
            directory.name.contains("common")
        ) "-rx" else ""

        if (file.name == "build.gradle" || file.name == "build.gradle.kts") {
            file.replaceDependencyToRemote("all$postFix")
            file.replaceDependencyToRemote("partner-all$postFix")
            file.replaceDependencyToRemote("partner-friend-delegate")
        }
    }
}

// 샘플앱이 SDK 모듈 없이도 빌드되도록 파일 수정
val editFiles by tasks.registering(Task::class) {
    doLast {
        File(fullSourcePath).listFiles()?.forEach {
            if (it.path.contains("sample")) {
                // 통합 모듈을 외부 저장소에서 불러오도록 수정
                replaceSampleDependency(it)
            } else if (it.path.contains("open")) {
                it.listFiles()?.forEach { directory ->
                    if (directory.name.contains("sample")) {
                        // 통합 모듈을 외부 저장소에서 불러오도록 수정
                        replaceSampleDependency(directory)
                    }
                }
            }
        }
    }
}

val zipProject by tasks.registering(Zip::class) {
    dependsOn(copyProject)
    dependsOn(editFiles)
    from(fullSourcePath)
    destinationDirectory.set(rootProject.layout.buildDirectory.get().asFile)
    archiveBaseName.set(Publish.projectName)
    archiveVersion.set(SdkVersions.version)
}

val dokkaHtmlCollector by tasks.getting(org.jetbrains.dokka.gradle.DokkaCollectorTask::class) {
    outputDirectory.set(rootProject.layout.buildDirectory.get().asFile.resolve("dokka"))

    // 프로젝트 이름 패턴에 따라 적절한 문서 디렉토리 이름 설정
    val moduleNameValue = if (subprojects.map { it.name }.any { it.contains("partner") }) {
        "kakao-partner-android-docs"
    } else {
        "kakao-open-android-docs"
    }

    moduleName.set(moduleNameValue)
}

val zipDokka by tasks.registering(Zip::class) {
    dependsOn(dokkaHtmlCollector)
    from("${rootProject.layout.buildDirectory.dir("dokka").get().asFile}")
    destinationDirectory.set(rootProject.layout.buildDirectory.get().asFile)
    archiveBaseName.set(Publish.dokkaArtifactId)
    archiveVersion.set(SdkVersions.version)
    delete("${rootProject.layout.buildDirectory.dir("dokka").get().asFile}")
}

// kts 에서 gradle.properties 에서 property 를 읽어보는 방식
val nexusSnapshotRepositoryUrl = project.properties["NEXUS_SNAPSHOT_REPOSITORY_URL"] as? String
val nexusReleaseRepositoryUrl = project.properties["NEXUS_RELEASE_REPOSITORY_URL"] as? String
val nexusUsername = project.properties["NEXUS_USERNAME"] as? String
val nexusPassword = project.properties["NEXUS_PASSWORD"] as? String

publishing {
    repositories {
        maven {
            url = if (SdkVersions.version.endsWith("-SNAPSHOT")) {
                nexusSnapshotRepositoryUrl?.let { uri(it) } ?: mavenLocal().url
            } else {
                nexusReleaseRepositoryUrl?.let { uri(it) } ?: mavenLocal().url
            }
            credentials {
                username = nexusUsername ?: ""
                password = nexusPassword ?: ""
            }
        }
    }
    publications {
        register("dokka", MavenPublication::class) {
            groupId = Publish.groupId
            artifactId = Publish.dokkaArtifactId
            version = SdkVersions.version
            artifact(zipDokka.get())
        }
        register("project", MavenPublication::class) {
            groupId = Publish.groupId
            artifactId = Publish.projectName
            version = SdkVersions.version
            artifact(zipProject.get())
        }
    }
}
