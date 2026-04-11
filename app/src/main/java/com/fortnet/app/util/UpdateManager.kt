package com.fortnet.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

class UpdateManager(private val context: Context) {
    private val client: OkHttpClient by lazy {
        val appCache = Cache(File(context.cacheDir, "http_cache"), 10 * 1024 * 1024)
        val bootstrapClient = OkHttpClient.Builder().cache(appCache).build()

        val dns = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(listOf(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            ))
            .build()

        OkHttpClient.Builder()
            .dns(dns)
            .cache(appCache)
            .build()
    }
    private val DOWNLOAD_TITLE = "FortNet Update"

    fun checkForUpdates(url: String, onResult: (UpdateInfo?) -> Unit) {
        FortLogger.d("Checking for updates via DoH: $url")
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                FortLogger.e("Update check failed (network)", e)
                onResult(null)
            }
// ... rest preserved

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        val update = UpdateInfo(
                            versionCode = json.getInt("versionCode"),
                            versionName = json.getString("versionName"),
                            downloadUrl = json.getString("downloadUrl"),
                            releaseNotes = json.optString("releaseNotes", "")
                        )
                        
                        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                        }

                        if (update.versionCode > currentVersion) {
                            onResult(update)
                        } else {
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            }
        })
    }

    fun downloadAndInstall(updateUrl: String) {
        try {
            // فتح رابط التحميل في المتصفح بدلاً من التثبيت التلقائي
            // هذا أكثر أماناً ولا يحتاج لصلاحية REQUEST_INSTALL_PACKAGES
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "جاري فتح رابط التحميل...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            FortLogger.e("Failed to open download link", e)
            Toast.makeText(context, "فشل في فتح الرابط: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
