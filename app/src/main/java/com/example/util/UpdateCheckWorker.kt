package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("drive_store_prefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("auto_update_check", true)) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        }

        NotificationHelper.init(applicationContext)

        try {
            val apiKey = prefs.getString("api_key", "AIzaSyA4ymjFIbuGVhFsKjxVV46RT-qWqNHNiY4") ?: "AIzaSyA4ymjFIbuGVhFsKjxVV46RT-qWqNHNiY4"
            val folderId = "1PBrhSIvDk0QrgNS6XeTeA1RDLFPeTqKV"
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = AppRepository(applicationContext, db.appDao)

            withTimeout(10000) {
                repository.syncJsonManifestFromDrive(folderId, apiKey)
                repository.syncUpdatesFromDrive(folderId, apiKey)
            }

            val apps = repository.allApps.first()
            prefs.edit().putString("cached_apps_list", apps.joinToString("|||") { app ->
                "${app.packageName}||${app.name}||${app.versionName}||${app.versionCode}||${app.driveModifiedTime}||${app.patchVersion}"
            }).apply()
            checkAndNotify(apps)
        } catch (_: Exception) {
            val appsJson = prefs.getString("cached_apps_list", "") ?: ""
            if (appsJson.isNotEmpty()) {
                val apps = appsJson.split("|||").mapNotNull { entry ->
                    val parts = entry.split("||")
                    if (parts.size < 6) return@mapNotNull null
                    CachedApp(parts[0], parts[1], parts[2], parts[3].toIntOrNull() ?: return@mapNotNull null, parts[4].toLongOrNull() ?: 0L, parts[5])
                }
                checkAndNotify(apps)
            }
        }
        return Result.success()
    }

    private fun checkAndNotify(apps: List<*>) {
        val installed = try {
            applicationContext.packageManager.getInstalledPackages(0).associate {
                it.packageName to PackageInfoCompat.getLongVersionCode(it).toInt()
            }
        } catch (_: Exception) { return }
        if (installed.isEmpty()) return

        val selfPkg = "com.mahmuduls.appstore"
        val prefs = applicationContext.getSharedPreferences("drive_store_prefs", Context.MODE_PRIVATE)

        for (item in apps) {
            val app = if (item is CachedApp) item else {
                val a = item as? com.example.data.local.AppItem ?: continue
                CachedApp(a.packageName, a.name, a.versionName, a.versionCode, a.driveModifiedTime, a.patchVersion)
            }
            if (!installed.containsKey(app.packageName)) continue
            try {
                val installedVerName = applicationContext.packageManager.getPackageInfo(app.packageName, 0).versionName ?: ""
                val installedVerCode = installed[app.packageName] ?: 0
                val installedPatch = getInstalledPatchVersion(app.packageName)
                val lastNotified = prefs.getLong("last_notified_${app.packageName}", 0L)

                if (installedVerName.isNotEmpty() && isVersionNewer(
                        app.versionName, installedVerName,
                        app.versionCode, installedVerCode,
                        app.driveModifiedTime, lastNotified,
                        app.patchVersion, installedPatch
                    ) && app.driveModifiedTime > lastNotified
                ) {
                    NotificationHelper.showUpdateNotification(applicationContext, app.name, app.versionName, app.packageName)
                    prefs.edit().putLong("last_notified_${app.packageName}", app.driveModifiedTime).apply()
                }
            } catch (_: Exception) { }
        }

        val driverVerName = prefs.getString("self_update_version_name", null)
        val driverVerCode = prefs.getInt("self_update_version_code", 0)
        if (driverVerCode > (installed[selfPkg] ?: 0) && driverVerName != null) {
            NotificationHelper.showUpdateNotification(applicationContext, "App Store", driverVerName, selfPkg)
        }
    }

    private data class CachedApp(
        val packageName: String, val name: String, val versionName: String,
        val versionCode: Int, val driveModifiedTime: Long, val patchVersion: String
    )

    private fun getInstalledPatchVersion(packageName: String): String {
        return applicationContext.getSharedPreferences("installed_patch_versions", Context.MODE_PRIVATE).getString(packageName, "") ?: ""
    }

    private fun isVersionNewer(
        driveVersion: String, installedVersion: String,
        driveCode: Int, installedCode: Int,
        driveModifiedTime: Long, lastNotifiedTime: Long,
        drivePatchVersion: String, installedPatchVersion: String
    ): Boolean {
        if (driveCode != installedCode) return driveCode > installedCode
        val driveParts = driveVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val installedParts = installedVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val hasHuge = { parts: List<Int> -> parts.any { it >= 10000 } }
        if (!hasHuge(driveParts) && !hasHuge(installedParts)) {
            val maxLen = maxOf(driveParts.size, installedParts.size)
            for (i in 0 until maxLen) {
                val d = driveParts.getOrElse(i) { 0 }; val ins = installedParts.getOrElse(i) { 0 }
                if (d != ins) return d > ins
            }
        }
        if (drivePatchVersion.isNotEmpty() && installedPatchVersion.isNotEmpty()) {
            val dp = drivePatchVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val ip = installedPatchVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(dp.size, ip.size)
            for (i in 0 until maxLen) { val d = dp.getOrElse(i) { 0 }; val ins = ip.getOrElse(i) { 0 }; if (d != ins) return d > ins }
        }
        return false
    }
}
