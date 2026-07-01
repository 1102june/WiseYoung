package com.wiseyoung.pro.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object CalendarPermissionHelper {

    val PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    fun hasCalendarPermission(context: Context): Boolean =
        PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * 캘린더 권한 확인 후 없으면 요청하고, 허용되면 [action] 실행.
     */
    @Composable
    fun rememberRunWithCalendarPermission(): ((() -> Unit) -> Unit) {
        val context = LocalContext.current
        var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) {
                pendingAction?.invoke()
            }
            pendingAction = null
        }

        return remember {
            { action: () -> Unit ->
                if (hasCalendarPermission(context)) {
                    action()
                } else {
                    pendingAction = action
                    launcher.launch(PERMISSIONS)
                }
            }
        }
    }
}
