package com.fortnet.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val packageName: String,
    val isBlocked: Boolean = false,
    val timerEndTime: Long = 0L
)

@Entity(
    tableName = "schedule_entries",
    primaryKeys = ["packageName", "dayOfWeek"]
)
data class ScheduleEntry(
    val packageName: String,
    val dayOfWeek: Int,
    val enabled: Boolean = false,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0
)

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsSync(): List<AppSetting>

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    suspend fun getSetting(packageName: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Update
    suspend fun updateSetting(setting: AppSetting)

    @Query("SELECT packageName FROM app_settings WHERE isBlocked = 1")
    suspend fun getBlockedPackages(): List<String>

    @Query("UPDATE app_settings SET timerEndTime = 0, isBlocked = 0 WHERE timerEndTime > 0 AND timerEndTime <= :currentTime")
    suspend fun expireTimers(currentTime: Long)
}

@Dao
interface ScheduleEntryDao {
    @Query("SELECT * FROM schedule_entries WHERE packageName = :packageName")
    fun getScheduleForApp(packageName: String): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries WHERE packageName = :packageName")
    suspend fun getScheduleForAppSync(packageName: String): List<ScheduleEntry>

    @Query("SELECT * FROM schedule_entries WHERE dayOfWeek = :dayOfWeek AND enabled = 1")
    suspend fun getActiveSchedulesForDay(dayOfWeek: Int): List<ScheduleEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<ScheduleEntry>)

    @Query("DELETE FROM schedule_entries WHERE packageName = :packageName")
    suspend fun deleteScheduleForApp(packageName: String)

    @Query("SELECT DISTINCT packageName FROM schedule_entries WHERE enabled = 1")
    suspend fun getPackagesWithSchedule(): List<String>

    @Query("SELECT DISTINCT packageName FROM schedule_entries WHERE enabled = 1")
    fun getPackagesWithScheduleFlow(): Flow<List<String>>
}

@Database(entities = [AppSetting::class, ScheduleEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
    abstract fun scheduleEntryDao(): ScheduleEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fortnet_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
