package com.fortnet.app.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.fortnet.app.data.AppDatabase
import com.fortnet.app.service.LocalVpnService
import java.util.concurrent.TimeUnit

class ScheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)

        // Expire finished timers
        db.appSettingDao().expireTimers(System.currentTimeMillis())

        // Restart VPN service to re-evaluate blocked apps based on schedules and timers
        val intent = Intent(applicationContext, LocalVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "fortnet_schedule",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
