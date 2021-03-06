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

package com.thanksmister.iot.mqtt.alarmpanel.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.thanksmister.iot.mqtt.alarmpanel.BaseActivity
import com.thanksmister.iot.mqtt.alarmpanel.R
import com.thanksmister.iot.mqtt.alarmpanel.ui.fragments.LogFragment
import kotlinx.android.synthetic.main.activity_logs.*

class LogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.show()
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.setTitle(R.string.activity_logs_title)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.contentFrame, LogFragment.newInstance(), LOGS_FRAGMENT).commit()
        }

        //resetInactivityTimer()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_logs
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * We should close this view if we have no more user activity.
     */
    override fun showScreenSaver(manuallySet: Boolean) {
        super.showScreenSaver(manuallySet)
        this.finish()
    }

    companion object {

        private val LOGS_FRAGMENT = "com.thanksmister.fragment.LOGS_FRAGMENT"

        fun createStartIntent(context: Context): Intent {
            return Intent(context, LogActivity::class.java)
        }
    }
}