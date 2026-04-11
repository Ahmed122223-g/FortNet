package com.fortnet.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.fortnet.app.data.AppDatabase
import com.fortnet.app.util.FortLogger
import kotlinx.coroutines.*
import java.util.*

class LocalVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            FortLogger.i("Starting Service...")
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FortNet نشط")
                .setContentText("يتم مراقبة وصول التطبيقات")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            startVpn()
        } catch (e: Exception) {
            FortLogger.e("Failed to start service notification", e)
        }
        return START_STICKY
    }

    private var currentBlockedApps: Set<String> = emptySet()

    private fun startVpn() {
        serviceScope.launch {
            while (isActive) {
                try {
                    checkAndApplyVpn()
                } catch (e: Exception) {
                    FortLogger.e("Error in VPN loop", e)
                }
                delay(30_000) // فحص كل 30 ثانية لزيادة الدقة
            }
        }
    }

    private suspend fun checkAndApplyVpn() {
        val db = AppDatabase.getDatabase(applicationContext)
        
        try {
            db.appSettingDao().expireTimers(System.currentTimeMillis())
        } catch (e: Exception) {
            FortLogger.e("Failed to expire timers", e)
        }
        
        val blockedApps = getEffectiveBlockedPackages(db)

        if (blockedApps == currentBlockedApps && vpnInterface != null) {
            return 
        }
        
        FortLogger.i("Applying new block list: ${blockedApps.size} apps")
        currentBlockedApps = blockedApps

        if (blockedApps.isEmpty()) {
            FortLogger.d("No apps to block, stopping VPN interface")
            closeVpn()
            return
        }

        // تطبيقات يجب ألا يتم حظرها أبداً لتجنب تعليق النظام
        val criticalApps = setOf(
            packageName,                  // تطبيقنا نفسه
            "com.android.vending",        // متجر جوجل
            "com.google.android.gms",     // خدمات جوجل
            "com.android.providers.media" // خدمات الميديا
        )

        // تصفية التطبيقات الحيوية من قائمة الحظر
        val safeBlockedApps = blockedApps.filter { it !in criticalApps }

        if (safeBlockedApps.isEmpty()) {
            FortLogger.d("No safe apps to block after filtering critical apps")
            closeVpn()
            return
        }

        val builder = Builder()
            .setSession("FortNet Firewall")
            .addAddress("10.0.0.1", 32)
            .addRoute("0.0.0.0", 0) // توجيه حركة المرور المختارة للـ VPN (Blackhole)
            .setBlocking(true)

        // مهم جداً: نستخدم addAllowedApplication فقط
        // هذا يعني أن التطبيقات المحظورة فقط تمر عبر الـ VPN (الثقب الأسود)
        // وباقي التطبيقات تتصل بالإنترنت بشكل طبيعي تماماً
        for (pkg in safeBlockedApps) {
            try {
                builder.addAllowedApplication(pkg)
                FortLogger.d("Blocking: $pkg")
            } catch (e: Exception) {
                FortLogger.w("Could not add $pkg to VPN: ${e.message}")
            }
        }

        try {
            closeVpn()
            vpnInterface = builder.establish()
            FortLogger.i("VPN Interface established - blocking ${safeBlockedApps.size} apps only")
        } catch (e: Exception) {
            FortLogger.e("CRITICAL: Failed to establish VPN interface", e)
            closeVpn()
        }
    }

    private fun closeVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            FortLogger.e("Error closing VPN", e)
        }
    }

    private suspend fun getEffectiveBlockedPackages(db: AppDatabase): Set<String> {
        return try {
            val currentTime = System.currentTimeMillis()
            val settings = db.appSettingDao().getAllSettingsSync()
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            val schedules = db.scheduleEntryDao().getActiveSchedulesForDay(dayOfWeek)
            val scheduledPackages = schedules.filter { entry ->
                val startMin = entry.startHour * 60 + entry.startMinute
                val endMin = entry.endHour * 60 + entry.endMinute
                currentMinutes in startMin until endMin
            }.map { it.packageName }.toSet()

            val blocked = mutableSetOf<String>()
            for (s in settings) {
                if (s.isBlocked) blocked.add(s.packageName)
                if (s.timerEndTime > currentTime) blocked.add(s.packageName)
            }
            blocked.addAll(scheduledPackages)
            blocked
        } catch (e: Exception) {
            FortLogger.e("Error calculating blocked packages", e)
            emptySet()
        }
    }

    override fun onDestroy() {
        FortLogger.i("Service Destroyed")
        super.onDestroy()
        serviceJob.cancel()
        closeVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "خدمة FortNet", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
    }
}
