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

package in.rahulja.materialhn.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import in.rahulja.materialhn.BuildConfig;

/**
 * App widget provider that handles widget lifecycle events. It is responsible for initializing,
 * updating, and removing widgets.
 */
public class WidgetProvider extends AppWidgetProvider {

  static final String ACTION_REFRESH_WIDGET = BuildConfig.APPLICATION_ID + ".ACTION_REFRESH_WIDGET";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (TextUtils.equals(intent.getAction(), ACTION_REFRESH_WIDGET)) {
      int appWidgetId =
          intent.getIntExtra(
              AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
      if (!isKnownWidget(context, appWidgetId)) {
        // Ignore ids this provider does not own; any app can send this broadcast.
        return;
      }
      final PendingResult pendingResult = goAsync();
      new Thread(
              () -> {
                new WidgetHelper(context).refresh(appWidgetId);
                pendingResult.finish();
              })
          .start();
    } else if (TextUtils.equals(intent.getAction(), AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
      int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
      if (appWidgetIds != null) {
        final PendingResult pendingResult = goAsync();
        new Thread(
                () -> {
                  WidgetHelper widgetHelper = new WidgetHelper(context);
                  for (int appWidgetId : appWidgetIds) {
                    widgetHelper.configure(appWidgetId);
                  }
                  pendingResult.finish();
                })
            .start();
      }
    } else {
      super.onReceive(context, intent);
    }
  }

  /**
   * True when the widget id belongs to an instance of this provider.
   */
  static boolean isKnownWidget(Context context, int appWidgetId) {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      return false;
    }
    int[] ids =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(new android.content.ComponentName(context, WidgetProvider.class));
    for (int id : ids) {
      if (id == appWidgetId) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final PendingResult pendingResult = goAsync();
    new Thread(
            () -> {
              WidgetHelper widgetHelper = new WidgetHelper(context);
              for (int appWidgetId : appWidgetIds) {
                widgetHelper.update(appWidgetId);
              }
              pendingResult.finish();
            })
        .start();
  }

  @Override
  public void onDeleted(Context context, int[] appWidgetIds) {
    WidgetHelper widgetHelper = new WidgetHelper(context);
    for (int appWidgetId : appWidgetIds) {
      widgetHelper.remove(appWidgetId);
    }
  }
}
