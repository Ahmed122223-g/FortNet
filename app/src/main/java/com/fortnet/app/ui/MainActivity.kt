package com.fortnet.app.ui

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.fortnet.app.service.LocalVpnService
import com.fortnet.app.ui.screens.AppListScreen
import com.fortnet.app.worker.ScheduleWorker

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel
    private var pendingAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = AppViewModel(applicationContext)
        ScheduleWorker.enqueue(applicationContext)

        setContent {
            Surface {
                AppListScreen(
                    viewModel = viewModel,
                    onToggle = { app ->
                        ensureVpnPermission {
                            viewModel.toggleBlock(app)
                        }
                    },
                    onTimerSet = { app, minutes ->
                        ensureVpnPermission {
                            viewModel.setTimer(app, minutes)
                        }
                    },
                    onTimerCancel = { app ->
                        viewModel.cancelTimer(app)
                        restartVpnService()
                    },
                    onBatteryOptimClick = {
                        requestBatteryOptimization()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkBatteryOptimization()
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun ensureVpnPermission(action: () -> Unit) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            pendingAction = action
            @Suppress("DEPRECATION")
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            action()
            restartVpnService()
        }
    }

    private fun restartVpnService() {
        val intent = Intent(this, LocalVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            pendingAction?.invoke()
            pendingAction = null
            restartVpnService()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val VPN_REQUEST_CODE = 100
    }
}
