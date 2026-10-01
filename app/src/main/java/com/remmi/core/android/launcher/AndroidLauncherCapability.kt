package com.remmi.core.android.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android platform implementation of [RemmiLauncherCapability].
 * Encapsulates [PackageManager] calls off the main thread and monitors package changes.
 */
class AndroidLauncherCapability(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : RemmiLauncherCapability {

    private val _installedApps = MutableStateFlow<List<RemmiAppInfo>>(emptyList())
    override val installedApps: StateFlow<List<RemmiAppInfo>> = _installedApps.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scope.launch {
                refreshInstalledApps()
            }
        }
    }

    init {
        registerPackageChangeReceiver()
    }

    private fun registerPackageChangeReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            context.applicationContext.registerReceiver(packageChangeReceiver, filter)
        } catch (e: Exception) {
            // Context registration safety
        }
    }

    override suspend fun refreshInstalledApps() {
        _isDiscovering.value = true
        try {
            val apps = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                    resolveInfos.mapNotNull { resolveInfo ->
                        val label = resolveInfo.loadLabel(pm).toString()
                        val packageName = resolveInfo.activityInfo.packageName
                        val activityName = resolveInfo.activityInfo.name
                        if (packageName.isNotEmpty() && activityName.isNotEmpty() && packageName != context.packageName) {
                            RemmiAppInfo(
                                packageName = packageName,
                                activityName = activityName,
                                label = label
                            )
                        } else null
                    }.sortedBy { it.label.lowercase() }
                } catch (e: Exception) {
                    _installedApps.value
                }
            }
            _installedApps.value = apps
        } finally {
            _isDiscovering.value = false
        }
    }

    override fun launchApp(appInfo: RemmiAppInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(appInfo.packageName, appInfo.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
