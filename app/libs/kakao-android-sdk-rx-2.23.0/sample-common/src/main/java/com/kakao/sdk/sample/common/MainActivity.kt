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
package com.kakao.sdk.sample.common

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.sample.common.internal.DebugFragment

class MainActivity : AppCompatActivity() {
    private val debugFragment = DebugFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 15 edge to edge 대응
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout)) { v, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                systemInsets.left,
                systemInsets.top,
                systemInsets.right,
                systemInsets.bottom
            )
            insets
        }

        setSupportActionBar(findViewById(R.id.toolbar))

        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()

        val bundle = Bundle().apply {
            putLong("customMemo", 19388)
            putLong("customMessage", 19388)
            putString("channelId", "_ZeUTxl")
            putString("calendarEventId", "63996425afcec577cce94f0b")
            putString("settleId", "f3318663-771a-4b24-8714-3f3061fa17cd")
            putString("targetAppKey", "28a88afd4adb1df9701d14e8e615ffaf")
        }

        if (KakaoSdk.type == KakaoSdk.Type.KOTLIN) {
            transaction.replace(R.id.container, OpenFragment.newInstance(bundle))
                .disallowAddToBackStack().commit()
        } else {
            transaction.replace(R.id.container, OpenFragmentRx.newInstance(bundle))
                .disallowAddToBackStack().commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (debugFragment.isAdded) {
            menuInflater.inflate(R.menu.menu_debug_view, menu)
        } else {
            menuInflater.inflate(R.menu.menu_main, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_debug -> {
                if (debugFragment.isAdded) return true
                supportFragmentManager.beginTransaction().addToBackStack("main")
                    .add(R.id.container, debugFragment).commit()
                return true
            }

            R.id.menu_clear -> {
                debugFragment.clearLogs()
                return true
            }

            R.id.menu_close -> {
                if (!debugFragment.isAdded) return true
                supportFragmentManager.beginTransaction().remove(debugFragment).commit()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
