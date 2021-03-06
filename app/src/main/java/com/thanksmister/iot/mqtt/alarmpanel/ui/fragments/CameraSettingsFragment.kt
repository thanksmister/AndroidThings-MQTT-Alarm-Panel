/*
 * Copyright (c) 2018. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.iot.mqtt.alarmpanel.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.view.View
import com.thanksmister.iot.mqtt.alarmpanel.BaseActivity

import com.thanksmister.iot.mqtt.alarmpanel.R
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.utils.DialogUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class CameraSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var configuration: Configuration
    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var mqttOptions: MQTTOptions

    private var tolPreference: EditTextPreference? = null
    private var fromPreference: EditTextPreference? = null
    private var domainPreference: EditTextPreference? = null
    private var keyPreference: EditTextPreference? = null
    private var activePreference: CheckBoxPreference? = null
    private var descriptionPreference: Preference? = null
    private var rotatePreference: ListPreference? = null
    private var telegramTokenPreference: EditTextPreference? = null
    private var telegramChatIdPreference: EditTextPreference? = null
    private var mqttImagePreference: CheckBoxPreference? = null
    private var mqttImageTopicPreference: EditTextPreference? = null
    private var notesPreference: Preference? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_camera)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        mqttImagePreference = findPreference(Configuration.PREF_MQTT_IMAGE) as CheckBoxPreference
        mqttImageTopicPreference = findPreference(Configuration.PREF_MQTT_IMAGE_TOPIC) as EditTextPreference
        telegramChatIdPreference = findPreference(Configuration.PREF_TELEGRAM_CHAT_ID) as EditTextPreference
        telegramTokenPreference = findPreference(Configuration.PREF_TELEGRAM_TOKEN) as EditTextPreference
        tolPreference = findPreference(Configuration.PREF_MAIL_TO) as EditTextPreference
        fromPreference = findPreference(Configuration.PREF_MAIL_FROM) as EditTextPreference
        domainPreference = findPreference(Configuration.PREF_MAIL_URL) as EditTextPreference
        keyPreference = findPreference(Configuration.PREF_MAIL_API_KEY) as EditTextPreference
        activePreference = findPreference(Configuration.PREF_MODULE_CAMERA) as CheckBoxPreference
        rotatePreference = findPreference(Configuration.PREF_CAMERA_ROTATE) as ListPreference
        descriptionPreference = findPreference("pref_mail_description")
        notesPreference = findPreference("pref_description")

        activePreference!!.isChecked = configuration.hasCamera()

        if (!TextUtils.isEmpty(configuration.getMailTo())) {
            tolPreference!!.text = configuration.getMailTo()
            tolPreference!!.summary = configuration.getMailTo()
        }

        if (!TextUtils.isEmpty(configuration.getMailFrom())) {
            fromPreference!!.text = configuration.getMailFrom()
            fromPreference!!.summary = configuration.getMailFrom()
        }

        if (!TextUtils.isEmpty(configuration.getMailGunUrl())) {
            domainPreference!!.text = configuration.getMailGunUrl()
            domainPreference!!.summary = configuration.getMailGunUrl()
        }

        if (!TextUtils.isEmpty(configuration.getMailGunApiKey())) {
            keyPreference!!.text = configuration.getMailGunApiKey()
            keyPreference!!.summary = configuration.getMailGunApiKey()
        }

        if (!TextUtils.isEmpty(configuration.telegramChatId)) {
            telegramChatIdPreference!!.text = configuration.telegramChatId
            telegramChatIdPreference!!.summary = configuration.telegramChatId
        }

        if (!TextUtils.isEmpty(configuration.telegramToken)) {
            telegramTokenPreference!!.text = configuration.telegramToken
            telegramTokenPreference!!.summary = configuration.telegramToken
        }

        if (!TextUtils.isEmpty(mqttOptions.getCameraTopic())) {
            mqttImageTopicPreference!!.text = mqttOptions.getCameraTopic()
            mqttImageTopicPreference!!.summary = mqttOptions.getCameraTopic()
        }

        mqttImagePreference!!.isChecked = configuration.mqttImage
        rotatePreference!!.setDefaultValue(configuration.getCameraRotate())
        rotatePreference!!.value = configuration.getCameraRotate().toString()
        if(configuration.getCameraRotate() == 0f) {
            rotatePreference!!.setValueIndex(0)
        } else if (configuration.getCameraRotate() == -90f) {
            rotatePreference!!.setValueIndex(1)
        } else if (configuration.getCameraRotate() == 90f) {
            rotatePreference!!.setValueIndex(2)
        } else if (configuration.getCameraRotate() == 180f) {
            rotatePreference!!.setValueIndex(3)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Configuration.PREF_MAIL_TO -> {
                val value = tolPreference!!.text
                configuration.setMailTo(value)
                tolPreference!!.summary = value
            }
            Configuration.PREF_MAIL_FROM -> {
                val value = fromPreference!!.text
                configuration.setMailFrom(value)
                fromPreference!!.summary = value
            }
            Configuration.PREF_MAIL_URL -> {
                val value = domainPreference!!.text
                configuration.setMailGunUrl(value)
                domainPreference!!.summary = value
            }
            Configuration.PREF_MAIL_API_KEY -> {
                val value = keyPreference!!.text
                configuration.setMailGunApiKey(value)
                keyPreference!!.summary = value
            }
            Configuration.PREF_TELEGRAM_CHAT_ID -> {
                val value = telegramChatIdPreference!!.text
                configuration.telegramChatId = value
                telegramChatIdPreference!!.summary = value
            }
            Configuration.PREF_TELEGRAM_TOKEN -> {
                val value = telegramTokenPreference!!.text
                configuration.telegramToken = value
                telegramTokenPreference!!.summary = value
            }
            Configuration.PREF_MODULE_CAMERA -> {
                val checked = activePreference!!.isChecked
                configuration.setHasCamera(checked)
            }
            Configuration.PREF_CAMERA_ROTATE -> {
                val valueFloat = rotatePreference!!.value
                val valueName = rotatePreference!!.entry.toString()
                rotatePreference!!.summary = getString(R.string.preference_camera_flip_summary, valueName)
                configuration.setCameraRotate(valueFloat)
            }
            Configuration.PREF_MQTT_IMAGE -> {
                val checked = mqttImagePreference!!.isChecked
                configuration.mqttImage = checked
            }
            Configuration.PREF_MQTT_IMAGE_TOPIC -> {
                val value = mqttImageTopicPreference!!.text
                mqttOptions!!.setCaptureTopic(value)
                mqttImageTopicPreference!!.summary = value
            }
        }
    }
}