/*
 * <!--
 *   ~ Copyright (c) 2017. ThanksMister LLC
 *   ~
 *   ~ Licensed under the Apache License, Version 2.0 (the "License");
 *   ~ you may not use this file except in compliance with the License. 
 *   ~ You may obtain a copy of the License at
 *   ~
 *   ~ http://www.apache.org/licenses/LICENSE-2.0
 *   ~
 *   ~ Unless required by applicable law or agreed to in writing, software distributed 
 *   ~ under the License is distributed on an "AS IS" BASIS, 
 *   ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   ~ See the License for the specific language governing permissions and 
 *   ~ limitations under the License.
 *   -->
 */

package com.thanksmister.iot.mqtt.alarmpanel

import android.arch.lifecycle.Observer
import android.content.pm.ActivityInfo
import android.content.res.Configuration.*
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatDelegate
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.android.things.device.TimeManager
import com.google.android.things.update.StatusListener
import com.google.android.things.update.UpdateManager
import com.google.android.things.update.UpdateManagerStatus
import com.google.android.things.update.UpdatePolicy.POLICY_APPLY_AND_REBOOT
import com.thanksmister.iot.mqtt.alarmpanel.managers.ConnectionLiveData
import com.thanksmister.iot.mqtt.alarmpanel.network.DarkSkyOptions
import com.thanksmister.iot.mqtt.alarmpanel.network.ImageOptions
import com.thanksmister.iot.mqtt.alarmpanel.persistence.DarkSkyDao
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.utils.DateUtils
import com.thanksmister.iot.mqtt.alarmpanel.utils.DeviceUtils
import com.thanksmister.iot.mqtt.alarmpanel.utils.DialogUtils
import dagger.android.support.DaggerAppCompatActivity
import dpreference.DPreference
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject lateinit var configuration: Configuration
    @Inject lateinit var preferences: DPreference
    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var darkSkyDataSource: DarkSkyDao

    private var inactivityHandler: Handler = Handler()
    private var hasNetwork = AtomicBoolean(true)
    val disposable = CompositeDisposable()
    private var connectionLiveData: ConnectionLiveData? = null

    abstract fun getLayoutId(): Int

    private val inactivityCallback = Runnable {
        showScreenSaver(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
    }

    override fun onStart(){
        super.onStart()
        setSystemInformation()
    }

    public override fun onResume() {
        super.onResume()
        if(configuration.nightModeChanged) {
            configuration.nightModeChanged = false // reset
            val handler = Handler()
            handler.postDelayed({ dayNightModeChanged() }, 1000)
        }

        val orientation = resources.configuration.orientation
        if(configuration.isPortraitMode && orientation == ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else if (!configuration.isPortraitMode && orientation == ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }

        val handler = Handler()
        handler.postDelayed({ setScreenBrightness() }, 1000)
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.removeCallbacks(inactivityCallback)
        disposable.dispose()
    }

    private fun setSystemInformation() {
        try {
            TimeManager.getInstance().setTimeZone(configuration.timeZone)
            UpdateManager.getInstance().addStatusListener(object: StatusListener {
                override fun onStatusUpdate(status: UpdateManagerStatus?) {
                    when (status?.currentState) {
                        UpdateManagerStatus.STATE_UPDATE_AVAILABLE -> {
                            Timber.d("Update available")
                            Toast.makeText(this@BaseActivity, getString(R.string.progress_updating), Toast.LENGTH_LONG).show()
                        }
                        UpdateManagerStatus.STATE_DOWNLOADING_UPDATE -> {
                            Timber.d("Update downloading")
                            Toast.makeText(this@BaseActivity, getString(R.string.progress_updating), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            });
            UpdateManager.getInstance().performUpdateNow(POLICY_APPLY_AND_REBOOT) // always apply update and reboot
        } catch (e:IllegalStateException) {
            Timber.e(e.message)
        }

        connectionLiveData = ConnectionLiveData(this)
        connectionLiveData?.observe(this, Observer { connected ->
            if(connected!!) {
                handleNetworkConnect()
            } else {
                handleNetworkDisconnect()
            }
        })
    }

    open fun dayNightModeCheck(dayNightMode:String?) {
        Timber.d("dayNightModeCheck")
        val uiMode = resources.configuration.uiMode and UI_MODE_NIGHT_MASK;
        if(dayNightMode == Configuration.DISPLAY_MODE_NIGHT && uiMode == UI_MODE_NIGHT_NO) {
            Timber.d("Tis the night!")
            setScreenBrightness()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            recreate()
        } else if (dayNightMode == Configuration.DISPLAY_MODE_DAY && uiMode == UI_MODE_NIGHT_YES) {
            Timber.d("Tis the day!")
            setScreenBrightness()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            recreate()
        }
    }

    private fun dayNightModeChanged() {
        Timber.d("dayNightModeChanged")
        val uiMode = resources.configuration.uiMode and UI_MODE_NIGHT_MASK;
        if (!configuration.useNightDayMode && uiMode == UI_MODE_NIGHT_YES) {
            Timber.d("Tis the day!")
            setScreenBrightness()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            recreate()
        }
    }

    /**
     * Resets the screen brightness to the default or based on time of day
     */
    private fun setScreenBrightness() {
        try {
            val brightness = getScreenBrightness()
            Timber.d("ScreenBrightness: $brightness")
            val lp: WindowManager.LayoutParams = window.attributes;
            lp.screenBrightness = brightness;
            window.attributes = lp
        } catch (e: IllegalStateException) {
            Timber.e("setScreenBrightness ${e.message}")
        }
    }

    /**
     * Returns the adjusted screen brightness depending on time of day or night mode.
     */
    private fun getScreenBrightness(): Float {
        val brightness: Float
        if(configuration.useNightDayMode && configuration.dayNightMode == Configuration.DISPLAY_MODE_NIGHT) {
            brightness = DeviceUtils.getScreenBrightnessNightMode(configuration.screenBrightness)
        } else {
            brightness = DeviceUtils.getScreenBrightnessBasedOnDayTime(configuration.screenBrightness,
                    DateUtils.getHourAndMinutesFromTimePicker(configuration.dayNightModeStartTime),
                    DateUtils.getHourAndMinutesFromTimePicker(configuration.dayNightModeEndTime))
        }
        return brightness
    }

    fun resetInactivityTimer() {
        Timber.d("resetInactivityTimer")
        dialogUtils.hideScreenSaverDialog()
        inactivityHandler.removeCallbacks(inactivityCallback)
        inactivityHandler.postDelayed(inactivityCallback, configuration.inactivityTime)
    }

    fun stopDisconnectTimer() {
        Timber.d("stopDisconnectTimer")
        dialogUtils.hideScreenSaverDialog()
        inactivityHandler.removeCallbacks(inactivityCallback)
    }

    override fun onUserInteraction() {
        Timber.d("onUserInteraction")
        resetInactivityTimer()
    }

    fun readWeatherOptions(): DarkSkyOptions {
        return DarkSkyOptions.from(preferences)
    }

    fun readImageOptions(): ImageOptions {
        return ImageOptions.from(preferences)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.itemId == android.R.id.home
    }

    /**
     * Show the screen saver only if the alarm isn't triggered. This shouldn't be an issue
     * with the alarm disabled because the disable time will be longer than this.
     */
    open fun showScreenSaver(manuallySet: Boolean) {
        if (!configuration.isAlarmTriggeredMode() && configuration.hasScreenSaver()) {
            Timber.d("showScreenSaver")
            inactivityHandler.removeCallbacks(inactivityCallback)
            val hasWeather = (configuration.showWeatherModule() && readWeatherOptions().isValid)
            dialogUtils.showScreenSaver(this@BaseActivity,
                    configuration.showPhotoScreenSaver(),
                    readImageOptions(),
                    getScreenBrightness(),
                    View.OnClickListener {
                        resetInactivityTimer()
                        setScreenBrightness()
                    }, darkSkyDataSource, hasWeather)
        } else if (manuallySet) {
            Timber.d("showBlackScreenSaver")
            dialogUtils.showBlackScreenSaver(this@BaseActivity,
                    View.OnClickListener {
                        Timber.d("Black Screen Clicked")
                        resetInactivityTimer()
                        setScreenBrightness()
                    })
        }
    }

    /**
     * On network disconnect show notification or alert message, clear the
     * screen saver and awake the screen. Override this method in activity
     * to for extra network disconnect handling such as bring application
     * into foreground.
     */
    open fun handleNetworkDisconnect() {
        dialogUtils.hideScreenSaverDialog()
        dialogUtils.showAlertDialogToDismiss(this@BaseActivity, getString(R.string.text_notification_network_title),
                    getString(R.string.text_notification_network_description))
        hasNetwork.set(false)
    }

    /**
     * On network connect hide any alert dialogs generated by
     * the network disconnect and clear any notifications.
     */
    open fun handleNetworkConnect() {
        dialogUtils.hideAlertDialog()
        hasNetwork.set(true)
    }

    open fun hasNetworkConnectivity(): Boolean {
        return hasNetwork.get()
    }
}