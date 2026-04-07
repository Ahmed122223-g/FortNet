package com.fortnet.app.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortnet.app.data.AppDatabase
import com.fortnet.app.data.AppSetting
import com.fortnet.app.data.ScheduleEntry
import com.fortnet.app.util.FortLogger
import com.fortnet.app.util.UpdateInfo
import com.fortnet.app.util.UpdateManager
import com.fortnet.app.util.ValidationUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppCategory(val label: String) {
    ALL("الكل"), SOCIAL("تواصل"), GAMES("ألعاب"),
    ENTERTAINMENT("ترفيه"), TOOLS("أدوات"), OTHER("أخرى")
}

enum class SortOption(val label: String) {
    NAME_ASC("الاسم أ-ي"), NAME_DESC("الاسم ي-أ"),
    INSTALL_NEW("الأحدث"), INSTALL_OLD("الأقدم")
}

enum class FilterOption(val label: String) {
    ALL("الكل"), BLOCKED("المحظورة"), UNBLOCKED("المسموحة")
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val isBlocked: Boolean,
    val icon: Drawable? = null,
    val installTime: Long = 0,
    val category: AppCategory = AppCategory.OTHER,
    val timerEndTime: Long = 0,
    val hasSchedule: Boolean = false
)

class AppViewModel(private val context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    
    private val _updateAvailable = MutableStateFlow<UpdateInfo?>(null)
    val updateAvailable: StateFlow<UpdateInfo?> = _updateAvailable

    val selectedCategory: StateFlow<AppCategory> = _selectedCategory
    val sortOption: StateFlow<SortOption> = _sortOption
    val filterOption: StateFlow<FilterOption> = _filterOption

    private val _isBatteryOptimized = MutableStateFlow(false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized

    init {
        checkForUpdates()
        checkBatteryOptimization()
    }

    fun checkBatteryOptimization() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        _isBatteryOptimized.value = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun checkForUpdates() {
        val manager = UpdateManager(context)
        val updateUrl = "https://raw.githubusercontent.com/Ahmed122223-g/FortNet/main/version.json"
        
        manager.checkForUpdates(updateUrl) { info ->
            if (info != null) {
                _updateAvailable.value = info
            }
        }
    }

    fun startUpdate() {
        _updateAvailable.value?.let { info ->
            val manager = UpdateManager(context)
            manager.downloadAndInstall(info.downloadUrl)
        }
    }

    private val settingsAndSchedules = combine(
        db.appSettingDao().getAllSettings(),
        db.scheduleEntryDao().getPackagesWithScheduleFlow()
    ) { settings, schedPkgs -> Pair(settings, schedPkgs) }

    val apps: StateFlow<List<AppInfo>> = combine(
        _searchQuery, _selectedCategory, _sortOption, _filterOption, settingsAndSchedules
    ) { query, category, sort, filter, (settings, schedPkgs) ->
        val pm = context.packageManager
        var list = pm.getInstalledPackages(0).filter {
            pm.getLaunchIntentForPackage(it.packageName) != null
        }.map { pkg ->
            val s = settings.find { it.packageName == pkg.packageName }
            AppInfo(
                name = pm.getApplicationLabel(pkg.applicationInfo).toString(),
                packageName = pkg.packageName,
                isBlocked = s?.isBlocked ?: false,
                icon = try { pm.getApplicationIcon(pkg.packageName) } catch (_: Exception) { null },
                installTime = pkg.firstInstallTime,
                category = categorizeApp(pkg.applicationInfo, pkg.packageName),
                timerEndTime = s?.timerEndTime ?: 0,
                hasSchedule = schedPkgs.contains(pkg.packageName)
            )
        }
        if (category != AppCategory.ALL) list = list.filter { it.category == category }
        list = when (filter) {
            FilterOption.ALL -> list
            FilterOption.BLOCKED -> list.filter { it.isBlocked }
            FilterOption.UNBLOCKED -> list.filter { !it.isBlocked }
        }
        if (query.isNotEmpty()) list = list.filter { it.name.contains(query, true) }
        when (sort) {
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortOption.INSTALL_NEW -> list.sortedByDescending { it.installTime }
            SortOption.INSTALL_OLD -> list.sortedBy { it.installTime }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setCategory(c: AppCategory) { _selectedCategory.value = c }
    fun setSortOption(o: SortOption) { _sortOption.value = o }
    fun setFilterOption(o: FilterOption) { _filterOption.value = o }

    fun toggleBlock(app: AppInfo) {
        if (!ValidationUtil.isValidPackageName(app.packageName)) {
            FortLogger.e("Invalid package name: ${app.packageName}")
            return
        }
        viewModelScope.launch {
            try {
                val cur = db.appSettingDao().getSetting(app.packageName)
                if (cur == null) {
                    db.appSettingDao().insertSetting(AppSetting(app.packageName, !app.isBlocked))
                } else {
                    db.appSettingDao().updateSetting(cur.copy(isBlocked = !app.isBlocked, timerEndTime = 0))
                }
                FortLogger.d("Toggled block for ${app.packageName}")
            } catch (e: Exception) {
                FortLogger.e("Failed to toggle block", e)
            }
        }
    }

    fun setTimer(app: AppInfo, durationMinutes: Int) {
        if (!ValidationUtil.isValidPackageName(app.packageName)) return
        viewModelScope.launch {
            try {
                val endTime = System.currentTimeMillis() + durationMinutes * 60 * 1000L
                val cur = db.appSettingDao().getSetting(app.packageName)
                if (cur == null) {
                    db.appSettingDao().insertSetting(AppSetting(app.packageName, isBlocked = true, timerEndTime = endTime))
                } else {
                    db.appSettingDao().updateSetting(cur.copy(isBlocked = true, timerEndTime = endTime))
                }
                FortLogger.d("Timer set for ${app.packageName}: $durationMinutes min")
            } catch (e: Exception) {
                FortLogger.e("Failed to set timer", e)
            }
        }
    }

    fun cancelTimer(app: AppInfo) {
        viewModelScope.launch {
            try {
                val cur = db.appSettingDao().getSetting(app.packageName)
                if (cur != null) {
                    db.appSettingDao().updateSetting(cur.copy(isBlocked = false, timerEndTime = 0))
                }
            } catch (e: Exception) {
                FortLogger.e("Failed to cancel timer", e)
            }
        }
    }

    fun getScheduleForApp(packageName: String): Flow<List<ScheduleEntry>> =
        db.scheduleEntryDao().getScheduleForApp(packageName)

    fun saveSchedule(packageName: String, entries: List<ScheduleEntry>) {
        if (!ValidationUtil.isValidPackageName(packageName)) return
        viewModelScope.launch {
            try {
                db.scheduleEntryDao().deleteScheduleForApp(packageName)
                db.scheduleEntryDao().insertEntries(entries)
                FortLogger.d("Schedule saved for $packageName")
            } catch (e: Exception) {
                FortLogger.e("Failed to save schedule", e)
            }
        }
    }

    private fun categorizeApp(info: ApplicationInfo, pkg: String): AppCategory {
        if (pkg in SOCIAL) return AppCategory.SOCIAL
        if (pkg in ENTERTAINMENT) return AppCategory.ENTERTAINMENT
        if (pkg in TOOLS) return AppCategory.TOOLS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return when (info.category) {
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.TOOLS
                else -> AppCategory.OTHER
            }
        }
        @Suppress("DEPRECATION")
        if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) return AppCategory.GAMES
        return AppCategory.OTHER
    }

    companion object {
        val SOCIAL = setOf(
            "com.whatsapp","com.whatsapp.w4b","org.telegram.messenger",
            "com.facebook.katana","com.facebook.lite","com.facebook.orca",
            "com.instagram.android","com.twitter.android","com.snapchat.android",
            "com.discord","com.skype.raider","com.viber.voip","com.linkedin.android"
        )
        val ENTERTAINMENT = setOf(
            "com.google.android.youtube","com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically","com.spotify.music",
            "com.netflix.mediaclient","tv.twitch.android.app"
        )
        val TOOLS = setOf(
            "com.google.android.gm","com.google.android.apps.docs",
            "com.google.android.calendar","com.android.chrome","org.mozilla.firefox"
        )
    }
}
