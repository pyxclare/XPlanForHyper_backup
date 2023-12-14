package com.itos.originplan

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.itos.originplan.ui.theme.Study_kotlinTheme
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnBinderReceivedListener
import rikka.shizuku.Shizuku.UserServiceArgs

data class AppInfo(
    var appName: String,
    val appPkg: String,
    var isDisabled: Boolean = false,
    var isExist: Boolean = true
)

var a: Boolean? = false
val pkglist: List<AppInfo> = listOf(
    AppInfo("mt", "bin.mt.plus.canary"),
    AppInfo("vivoopt", "com.itos.optzimiation"),
)


class MainActivity : ComponentActivity() {
    private val context: Context = this
    var userService: IUserService? = null

    val REQUEST_CODE = 123;
    val userServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            UserService::class.java.name
        )
    ).processNameSuffix("service")
    val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (service.pingBinder()) {
                userService = IUserService.Stub.asInterface(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }
    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            this.onRequestPermissionsResult(
                requestCode,
                grantResult
            )
        }
    private val BINDER_RECEVIED_LISTENER =
        object : OnBinderReceivedListener {
            override fun onBinderReceived() {
                Toast.makeText(
                    context,
                    ShizukuHelper.checkPermission().toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private val BINDER_DEAD_LISTENER: Shizuku.OnBinderDeadListener =
        object : Shizuku.OnBinderDeadListener {
            override fun onBinderDead() {
                Toast.makeText(
                    context,
                    ShizukuHelper.checkPermission().toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Study_kotlinTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppListContent()
                }
            }
        }
//        ShizukuHelper.requestPermission {
//            Toast.makeText(context, ShizukuHelper.checkPermission().toString(), Toast.LENGTH_SHORT).show()
//        }
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        try {
            if (checkPermission(REQUEST_CODE)) {
                //onGranted()
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (_: Exception) {
        }
        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEVIED_LISTENER)
        Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER)
        Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        Toast.makeText(context, ShizukuHelper.checkPermission().toString(), Toast.LENGTH_SHORT)
            .show()
    }

    private fun checkPermission(code: Int): Boolean {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            return false
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Users choose "Deny and don't ask again"
            return false
        } else {
            return false
        }
    }

    @Preview(showBackground = true)
    @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun GreetingPreview() {
        Study_kotlinTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                //SetTitle("原计划")
                AppListContent()
            }
        }
    }

    fun test(isDisabled: MutableState<Boolean>, packagename: String) {
        Toast.makeText(context, packagename+": "+isDisabled.value.toString(), Toast.LENGTH_SHORT).show()
        //userService?.setApplicationEnabled(packagename, isDisabled.value)
        HShizuku.setAppDisabled(packagename, !isDisabled.value)
        isDisabled.value = isAppDisabled( packagename)!!
        Toast.makeText(context, isDisabled.value.toString(), Toast.LENGTH_SHORT).show()
        //TODO 在这里修改isDisabled
        //TODO 完善Shizuku
    }
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: MainActivity private set
    }
    private fun onRequestPermissionsResult(requestCode: Int, grantResult: Int) {
        Toast.makeText(context, ShizukuHelper.checkPermission().toString(), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
        Shizuku.removeBinderReceivedListener(BINDER_RECEVIED_LISTENER)
        Shizuku.removeBinderDeadListener(BINDER_DEAD_LISTENER)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }


    @Composable
    fun AppListItem(appInfo: AppInfo) {
        //让 compose监听这个的变化
        var isDisabled = remember { mutableStateOf(appInfo.isDisabled) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左边显示应用名称
            Column {
                Text(text = appInfo.appName, style = MaterialTheme.typography.bodyMedium)
                Text(text = appInfo.appPkg, style = MaterialTheme.typography.bodySmall)
            }

            // 中间显示禁用状态文本
            Text(
                text = if (isDisabled.value) "Disabled" else "Enabled",
                color = if (isDisabled.value) Color.Red else Color.Green,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 16.dp)
            )

            // 右边是一个按钮
            IconButton(
                onClick = { test(isDisabled, appInfo.appPkg) }
            ) {
                // Toast.makeText(LocalContext.current, appInfo.appName, Toast.LENGTH_SHORT).show()
                val icon: ImageVector = if (appInfo.isExist && isDisabled.value) {
                    Icons.Default.Check
                } else if (appInfo.isExist) {
                    Icons.Default.Close
                } else {
                    Icons.Default.Warning
                }
                // icon = if (isDisabled) Icons.Default.Check else Icons.Default.Close
                Icon(
                    imageVector = icon,
                    contentDescription = if (isDisabled.value) "Enable" else "Disable"
                )
            }
        }
    }


    fun getAppNameByPackageName(context: Context, packageName: String): String {
        val packageManager: PackageManager = context.packageManager
        val applicationInfo: ApplicationInfo? = try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        return applicationInfo?.let {
            packageManager.getApplicationLabel(it).toString()
        } ?: "Unknown App"
    }

    @Composable
    fun AppList(appList: List<AppInfo>) {
        LazyColumn {
            items(appList) { appInfo ->
                AppListItem(
                    appInfo = appInfo
                )
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppListScreen(context: Context) {
        val appList = remember { generateAppList(context) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "OriginPlan") },
//                 backgroundColor = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Column {
                // TopAppBar
                TopAppBar(
                    title = { Text(text = "OriginPlan") },
                    // backgroundColor = MaterialTheme.colorScheme.primary
                )

                // AppList
                AppList(appList = appList)
            }
        }

    }

    @Composable
    fun AppListContent() {
        AppListScreen(LocalContext.current)
    }

    fun generateAppList(context: Context): List<AppInfo> {
        // 这里添加你的应用信息
        // 返回一个长度为10的列表，其中包含10个AppInfo对象
        for (appinfo in pkglist) {
            a = isAppDisabled(appinfo.appPkg)
            if (a != null) {
                appinfo.isDisabled = a as Boolean
            } else {
                appinfo.isDisabled = false
                appinfo.isExist = false
            }
            appinfo.appName = getAppNameByPackageName(context, appinfo.appPkg)
        }
        val testlist: List<AppInfo> = List(2) { index ->
            AppInfo(
                appName = "App $index",
                appPkg = "com.example.app$index",
                isDisabled = index % 2 == 0
            )
        }
        Log.d("列表项", pkglist.toString() + "\n" + testlist.toString())
        return pkglist
    }


    fun isAppDisabled(appPackageName: String): Boolean? {
        val packageManager: PackageManager = context.packageManager

        try {
            val applicationEnabledSetting: Int =
                packageManager.getApplicationEnabledSetting(appPackageName)

            // 应用被停用或者处于默认状态（未设置启用状态），返回 true；其他状态返回 false
            if (applicationEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                return true
            } else if (applicationEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || applicationEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return false
            }
        } catch (e: Exception) {
            // 如果找不到应用信息，也可以视为应用被停用
            return null
        }
        return true
    }


}



