package net.focustation.myapplication

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.naver.maps.map.NaverMapSdk
import net.focustation.myapplication.navigation.AppNavGraph
import net.focustation.myapplication.ui.theme.FocustationTheme

private const val NAVER_MAP_MCP_ID_META_KEY = "com.naver.maps.map.MCP_ID"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val naverMapMcpId =
            runCatching {
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                appInfo.metaData?.getString(NAVER_MAP_MCP_ID_META_KEY).orEmpty().trim()
            }.onFailure { error ->
                Log.e("NaverMap", "Failed to read NAVER_MAP_MCP_ID from manifest metadata", error)
            }.getOrDefault("")

        if (naverMapMcpId.isEmpty()) {
            Log.w("NaverMap", "NAVER_MAP_MCP_ID is empty. Check local.properties or gradle.properties.")
        } else {
            runCatching {
                NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient(naverMapMcpId)
                Log.d("NaverMap", "NaverMapSdk Client configured successfully")
            }.onFailure { e ->
                Log.e("NaverMap", "Failed to configure NaverMapSdk Client", e)
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
