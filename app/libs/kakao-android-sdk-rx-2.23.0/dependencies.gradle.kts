import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

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
apply(plugin = "com.android.library")

// 모든 라이브러리들이 공통으로 import 하는 라이브러리는 테스트 라이브러리 밖에 없다. 테스트 라이브러리는 써드 파티 앱에 포함이 되지 않으니까 공통으로 지정해도 큰 이슈 없음.
dependencies {
    val jupiter = "5.11.4"
    val robolectric = "4.14.1"
    val testCoreKtx = "1.6.1"
    val testRunner = "1.6.2"
    val testExtJunit = "1.2.1"
    val espresso = "3.6.1"
    val archCoreTesting = "2.2.0"

    "testImplementation"("org.junit.jupiter:junit-jupiter-api:$jupiter")
    "testImplementation"("org.junit.jupiter:junit-jupiter-params:$jupiter")
    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$jupiter")
    "testRuntimeOnly"("org.junit.vintage:junit-vintage-engine:$jupiter")
    "testImplementation"("org.robolectric:robolectric:$robolectric")
    "testImplementation"(Dependencies.mockwebserver)
    "testImplementation"(Dependencies.mockitoCore)
    "testImplementation"(Dependencies.mockitoInline)
    "testImplementation"("androidx.test:core-ktx:$testCoreKtx")
    "testImplementation"("androidx.test:runner:$testRunner")
    "testImplementation"("androidx.test.ext:junit-ktx:$testExtJunit")
    "testImplementation"("androidx.arch.core:core-testing:$archCoreTesting")

    "androidTestImplementation"(Dependencies.mockwebserver)
    "androidTestImplementation"(Dependencies.mockitoCore)
    "androidTestImplementation"(Dependencies.mockitoInline)
    "androidTestImplementation"("androidx.test:core-ktx:$testCoreKtx")
    "androidTestImplementation"("androidx.test:runner:$testRunner")
    "androidTestImplementation"("androidx.test.ext:junit-ktx:$testExtJunit")
    "androidTestImplementation"("androidx.test.espresso:espresso-core:$espresso")
    "androidTestImplementation"("androidx.test.espresso:espresso-contrib:$espresso")
    "androidTestImplementation"("androidx.arch.core:core-testing:$archCoreTesting")
}
