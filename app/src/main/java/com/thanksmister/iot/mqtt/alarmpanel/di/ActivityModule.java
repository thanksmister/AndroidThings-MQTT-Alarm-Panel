/*
 * Copyright (c) 2017. ThanksMister LLC
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

package com.thanksmister.iot.mqtt.alarmpanel.di;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.LocationManager;
import android.view.LayoutInflater;

import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions;
import com.thanksmister.iot.mqtt.alarmpanel.utils.DialogUtils;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dpreference.DPreference;

@Module
class ActivityModule {

    @Provides
    static DialogUtils providesDialogUtils(Application application) {
        return new DialogUtils(application);
    }

    @Provides
    static Resources providesResources(Application application) {
        return application.getResources();
    }

    @Provides
    static LayoutInflater providesInflater(Application application) {
        return (LayoutInflater) application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Provides
    static DPreference providePreferences(Application application) {
        return new DPreference(application.getApplicationContext(), "alarm_preferences");
    }

    @Provides
    static LocationManager provideLocationManager(Application application) {
        return (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
    }

    @Provides
    static Configuration provideConfiguration() {
        return new Configuration();
    }

    @Provides
    static MQTTOptions provideMQTTOptions(DPreference preference) {
        return new MQTTOptions(preference);
    }
}