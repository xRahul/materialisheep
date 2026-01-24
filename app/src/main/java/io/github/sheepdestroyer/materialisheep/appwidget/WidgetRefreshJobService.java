/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sheepdestroyer.materialisheep.appwidget;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.sheepdestroyer.materialisheep.MaterialisticApplication;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;

import static io.github.sheepdestroyer.materialisheep.DataModule.ALGOLIA;
import static io.github.sheepdestroyer.materialisheep.DataModule.HN;

public class WidgetRefreshJobService extends JobService {
    @Inject
    @Named(HN)
    ItemManager mItemManager;
    @Inject
    @Named(ALGOLIA)
    ItemManager mSearchManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ((MaterialisticApplication) getApplication()).applicationComponent.inject(this);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        int appWidgetId = jobParameters.getExtras().getInt(WidgetHelper.EXTRA_APP_WIDGET_ID, jobParameters.getJobId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            new Thread(() -> new WidgetHelper(this).refreshApi31(appWidgetId,
                    mItemManager, mSearchManager, () -> jobFinished(jobParameters, false))).start();
        } else {
            new WidgetHelper(this).refresh(appWidgetId);
            jobFinished(jobParameters, false); // if we're able to start job means we have network conn
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}
