package com.example.timetable.utils

import android.content.Context

fun getVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "-.-"
    } catch (_: Exception) {
        "_._"
    }
}