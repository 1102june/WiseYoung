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
apply(plugin = "jacoco")

tasks {
    val jacocoTestReport by registering(JacocoReport::class) {

        val libraries = subprojects.filter { it.name != "sample" && it.name != "sample-common" }
        libraries.forEach { dependsOn("${it.name}:jacocoTestReport") }
        reports {
            html.isEnabled = true
            html.destination = File("${rootProject.buildDir}/coverage-report")
        }
        group = "Reporting"
        description = "Generate Jacoco coverage reports."


        val filters = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*"
        )

        val sources = libraries.map { "${it.buildDir}/src/main/java" }
            .map { fileTree("dir" to it, "excludes" to filters) }
        val javaClasses = libraries.map { "${it.buildDir}/intermediates/javac/debug" }
            .map { fileTree("dir" to it, "excludes" to filters) }
        val kotlinClasses = libraries.map { "${it.buildDir}/tmp/kotlin-classes/debug" }
            .map { fileTree("dir" to it, "excludes" to filters) }
        val execution = libraries.map { it.buildDir }.map {
            fileTree(
                "dir" to it,
                "includes" to listOf(
                    "jacoco/testDebugUnitTest.exec",
                    "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
                )
            )
        }
        sourceDirectories.setFrom(sources)
        classDirectories.setFrom(kotlinClasses)
        executionData.setFrom(execution)
        doLast {
            println("Jacoco report generated at file://${reports.html.destination}/index.html")
        }
    }
}