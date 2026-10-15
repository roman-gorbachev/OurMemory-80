package com.gorman.ourmemoryapp

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.gorman.ourmemoryapp.appcheck.AppCheckProviders
import com.gorman.ourmemoryapp.domain.repository.SettingsRepository
import com.gorman.ourmemoryapp.reminders.FavoriteAnniversaryScheduler
import com.gorman.ourmemoryapp.reminders.VictoryDayReminderScheduler
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Firebase.appCheck.installAppCheckProviderFactory(AppCheckProviders.factory())
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
        FavoriteAnniversaryScheduler.schedule(this, Clock.systemDefaultZone())
        appScope.launch {
            if (settingsRepository.observeSettings().first().victoryDayReminder) {
                VictoryDayReminderScheduler.schedule(this@MyApp, Clock.systemDefaultZone())
            }
        }
    }
}
