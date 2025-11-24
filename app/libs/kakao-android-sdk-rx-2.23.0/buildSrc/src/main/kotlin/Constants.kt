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
import java.io.File
import java.util.Properties

object Publish {
    const val projectName = "kakao-android-sdk-rx"
    const val groupId = "com.kakao.sdk"
    const val dokkaArtifactId = "${projectName}-doc"

    val samples: List<String>
        get() = with(java.util.Properties()) {
            loadFile("modules.properties").inputStream().use { load(it) }
            getProperty("OPEN_SAMPLES").split(",")
        }

    val excludedLibraries: List<String> = emptyList()
}

object Dokka {
    val samples: List<String>
        get() = with(Properties()) {
            loadFile("modules.properties").inputStream().use { load(it) }
            getProperty("OPEN_SAMPLES").split(",")
        }
}

fun loadFile(name: String): File {
    return if (File(name).exists()) {
        File(name)
    }else {
        File("../$name")
    }
}
