package com.wiseyoung.pro.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.wiseyoung.pro.data.model.PolicyResponse

fun resolveApplicationLink(link1: String?, link2: String?): String? =
    link2?.takeIf { it.isNotBlank() } ?: link1?.takeIf { it.isNotBlank() }

fun PolicyResponse.applicationLink(): String? = resolveApplicationLink(link1, link2)

fun openApplicationLink(context: Context, url: String?) {
    if (url.isNullOrBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}
