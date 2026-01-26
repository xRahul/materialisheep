/*
 * Copyright (c) 2023 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sheepdestroyer.materialisheep;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Typeface;
import androidx.appcompat.app.AppCompatDelegate;
import android.os.StrictMode;
import io.github.sheepdestroyer.materialisheep.data.AlgoliaClient;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MaterialisticApplication extends android.app.Application {
    private static volatile Typeface sTypeface = null;
    public ApplicationComponent applicationComponent;

    public static Typeface getTypeface(android.content.Context context) {
        if (sTypeface == null) {
            synchronized (MaterialisticApplication.class) {
                if (sTypeface == null) {
                    sTypeface = FontCache.getInstance().get(context, Preferences.Theme.getTypeface(context));
                }
            }
        }
        return sTypeface;
    }

    public static void setTypeface(Typeface typeface) {
        sTypeface = typeface;
    }

    @Override
    @SuppressLint("DefaultUncaughtExceptionDelegation") // We want to show a custom crash activity and not delegate to system default
    public void onCreate() {
        super.onCreate();
        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        AppCompatDelegate.setDefaultNightMode(Preferences.Theme.getAutoDayNightMode(this));
        AlgoliaClient.sSortByTime = Preferences.isSortByRecent(this);
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());

            Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                String stackTrace = sw.toString();

                android.content.Intent intent = new android.content.Intent(this, CrashActivity.class);
                intent.putExtra(CrashActivity.EXTRA_LOG, stackTrace);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                System.exit(1);
            });
        }
        Preferences.migrate(this);
        AppUtils.registerAccountsUpdatedListener(this);
        AdBlocker.init(this, Schedulers.io());
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(new io.github.sheepdestroyer.materialisheep.data.ItemSyncNetworkCallback(this));
    }
}
