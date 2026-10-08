package com.opencallshield

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.opencallshield.sync.SyncWorker
import java.util.concurrent.TimeUnit

class OpenCallShieldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Mantiene in italiano anche i testi forniti da Android e dalle librerie.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            val italian = LocaleList.forLanguageTags("it")
            if (localeManager.applicationLocales != italian) {
                localeManager.applicationLocales = italian
            }
        }
        scheduleDailySync()
    }

    private fun scheduleDailySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ocs_daily_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

