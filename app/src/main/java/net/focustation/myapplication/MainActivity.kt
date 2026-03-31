package net.focustation.myapplication

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.naver.maps.map.NaverMapSdk
import net.focustation.myapplication.navigation.AppNavGraph
import net.focustation.myapplication.ui.theme.FocustationTheme
import net.focustation.myapplication.util.DebugLog

private const val NAVER_MAP_MCP_ID_META_KEY = "com.naver.maps.map.MCP_ID"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val naverMapMcpId =
            runCatching {
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                appInfo.metaData
                    ?.getString(NAVER_MAP_MCP_ID_META_KEY)
                    .orEmpty()
                    .trim()
            }.onFailure { error ->
                DebugLog.e("Failed to read NAVER_MAP_MCP_ID from manifest metadata", error)
            }.getOrDefault("")

        if (naverMapMcpId.isEmpty()) {
            DebugLog.w("NAVER_MAP_MCP_ID is empty. Check local.properties or gradle.properties.")
        } else {
            runCatching {
                NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient(naverMapMcpId)
                DebugLog.d("NaverMapSdk Client configured successfully")
            }.onFailure { e ->
                DebugLog.e("Failed to configure NaverMapSdk Client", e)
            }
        }

        setContent {
            FocustationTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
