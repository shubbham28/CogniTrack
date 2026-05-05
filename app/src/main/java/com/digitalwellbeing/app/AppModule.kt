package com.digitalwellbeing.app

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import com.digitalwellbeing.analytics.DailySummaryCalculator
import com.digitalwellbeing.analytics.DigitalFitnessTranslator
import com.digitalwellbeing.analytics.PickupCounter
import com.digitalwellbeing.analytics.SessionStitcher
import com.digitalwellbeing.capture.PermissionSnapshot
import com.digitalwellbeing.capture.PermissionStateEvaluator
import com.digitalwellbeing.storage.DigitalWellbeingDatabase
import java.time.ZoneId

object AppModule {
    @Volatile
    private var database: DigitalWellbeingDatabase? = null

    fun provideDatabase(context: Context): DigitalWellbeingDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                DigitalWellbeingDatabase::class.java,
                "cognitrack.db"
            ).build().also { database = it }
        }
    }

    fun provideDao(context: Context) = provideDatabase(context).wellbeingDao()

    fun provideRepository(context: Context): DashboardRepository {
        val appContext = context.applicationContext
        val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = appContext.packageManager
        val database = provideDatabase(appContext)

        return DashboardRepository(
            usageStatsCollector = UsageStatsCollector(usageStatsManager),
            usageSummaryReader = UsageSummaryReader(usageStatsManager),
            permissionGateway = PermissionGateway(appContext),
            dao = database.wellbeingDao(),
            packageManager = packageManager,
            sessionStitcher = SessionStitcher(),
            pickupCounter = PickupCounter(),
            summaryCalculator = DailySummaryCalculator(ZoneId.systemDefault()),
            translator = DigitalFitnessTranslator()
        )
    }
}

class PermissionGateway(
    private val context: Context
) {
    private val evaluator = PermissionStateEvaluator()

    fun snapshot(advancedModeEnabled: Boolean = false): PermissionSnapshot {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return PermissionSnapshot(
            hasUsageAccess = mode == AppOpsManager.MODE_ALLOWED,
            hasNotificationAccess = false,
            hasAccessibilityAccess = false,
            advancedModeEnabled = advancedModeEnabled
        )
    }

    fun hasUsageAccess(): Boolean = evaluator.evaluate(snapshot()) != com.digitalwellbeing.capture.PermissionState.NEEDS_USAGE_ACCESS
}
