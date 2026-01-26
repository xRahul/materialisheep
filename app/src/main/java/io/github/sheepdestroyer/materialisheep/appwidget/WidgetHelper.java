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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import io.github.sheepdestroyer.materialisheep.AppUtils;
import io.github.sheepdestroyer.materialisheep.BestActivity;
import io.github.sheepdestroyer.materialisheep.ListActivity;
import io.github.sheepdestroyer.materialisheep.NewActivity;
import io.github.sheepdestroyer.materialisheep.R;
import io.github.sheepdestroyer.materialisheep.SearchActivity;
import io.github.sheepdestroyer.materialisheep.data.Item;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * A helper class for managing widgets.
 */
class WidgetHelper {
    private static final String SP_NAME = "WidgetConfiguration_%1$d";
    private static final int DEFAULT_FREQUENCY_HOUR = 6;
    static final String EXTRA_APP_WIDGET_ID = "APP_WIDGET_ID";
    private static final int IMMEDIATE_JOB_ID_OFFSET = 1000000;
    private final Context mContext;
    private final AppWidgetManager mAppWidgetManager;
    private final AlarmManager mAlarmManager;

    /**
     * Constructs a new {@code WidgetHelper}.
     *
     * @param context the application context
     */
    WidgetHelper(Context context) {
        mContext = context;
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    }

    /**
     * Gets the name of the shared preferences file for a given widget ID.
     *
     * @param appWidgetId the widget ID
     * @return the shared preferences file name
     */
    static String getConfigName(int appWidgetId) {
        return String.format(Locale.US, SP_NAME, appWidgetId);
    }

    /**
     * Configures a widget after it has been created.
     *
     * @param appWidgetId the ID of the widget to configure
     */
    void configure(int appWidgetId) {
        scheduleUpdate(appWidgetId);
        update(appWidgetId);
    }

    /**
     * Updates the widget's views.
     *
     * @param appWidgetId the ID of the widget to update
     */
    void update(int appWidgetId) {
        WidgetConfig config = WidgetConfig.createWidgetConfig(mContext,
                getConfig(appWidgetId, R.string.pref_widget_theme),
                getConfig(appWidgetId, R.string.pref_widget_section),
                getConfig(appWidgetId, R.string.pref_widget_query));
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), config.widgetLayout);
        updateTitle(remoteViews, config);
        updateCollection(appWidgetId, remoteViews, config);
        mAppWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PersistableBundle extras = new PersistableBundle();
            extras.putInt(EXTRA_APP_WIDGET_ID, appWidgetId);
            getJobScheduler().schedule(new JobInfo.Builder(appWidgetId + IMMEDIATE_JOB_ID_OFFSET,
                    new ComponentName(mContext.getPackageName(), WidgetRefreshJobService.class.getName()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(extras)
                    .setOverrideDeadline(0)
                    .build());
        }
    }

    /**
     * Refreshes the data in the widget's list view.
     *
     * @param appWidgetId the ID of the widget to refresh
     */
    @SuppressWarnings("deprecation")
    void refresh(int appWidgetId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            mAppWidgetManager.notifyAppWidgetViewDataChanged(new int[]{appWidgetId}, android.R.id.list);
        }
        update(appWidgetId);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    void refreshApi31(int appWidgetId, ItemManager itemManager, ItemManager searchManager, Runnable onComplete) {
        WidgetConfig config = WidgetConfig.createWidgetConfig(mContext,
                getConfig(appWidgetId, R.string.pref_widget_theme),
                getConfig(appWidgetId, R.string.pref_widget_section),
                getConfig(appWidgetId, R.string.pref_widget_query));

        String filter;
        int hotThreshold;
        ItemManager manager;
        if (config.customQuery) {
            manager = searchManager;
            filter = config.section;
            hotThreshold = AppUtils.HOT_THRESHOLD_NORMAL;
        } else if (TextUtils.equals(config.section,
                mContext.getString(R.string.pref_widget_section_value_best))) {
            manager = itemManager;
            filter = ItemManager.BEST_FETCH_MODE;
            hotThreshold = AppUtils.HOT_THRESHOLD_HIGH;
        } else if (TextUtils.equals(config.section,
                mContext.getString(R.string.pref_widget_section_value_top))) {
            manager = itemManager;
            filter = ItemManager.TOP_FETCH_MODE;
            hotThreshold = AppUtils.HOT_THRESHOLD_NORMAL;
        } else {
            manager = itemManager;
            filter = config.section;
            hotThreshold = AppUtils.HOT_THRESHOLD_NORMAL;
        }

        Item[] items = manager.getStories(filter, ItemManager.MODE_NETWORK);
        RemoteViews.RemoteCollectionItems.Builder itemsBuilder = new RemoteViews.RemoteCollectionItems.Builder();

        if (items != null) {
            int count = Math.min(items.length, 10);
            List<String> idsToFetch = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Item item = items[i];
                if (item != null && item.getLocalRevision() <= 0) {
                    idsToFetch.add(item.getId());
                }
            }
            if (!idsToFetch.isEmpty()) {
                Item[] remoteItems = manager.getItems(idsToFetch.toArray(new String[0]), ItemManager.MODE_NETWORK);
                Map<String, Item> remoteItemMap = new HashMap<>();
                if (remoteItems != null) {
                    for (Item remoteItem : remoteItems) {
                        if (remoteItem != null) {
                            remoteItemMap.put(remoteItem.getId(), remoteItem);
                        }
                    }
                }

                for (int i = 0; i < count; i++) {
                    Item item = items[i];
                    if (item != null && item.getLocalRevision() <= 0) {
                        Item remoteItem = remoteItemMap.get(item.getId());
                        if (remoteItem != null) {
                            item.populate(remoteItem);
                        }
                    }
                }
            }

            for (int i = 0; i < count; i++) {
                Item item = items[i];
                if (item != null && item.getLocalRevision() > 0) {
                     RemoteViews itemView = new RemoteViews(mContext.getPackageName(),
                            config.isLightTheme ? R.layout.item_widget_light : R.layout.item_widget);
                     bindItem(mContext, itemView, item, hotThreshold);
                     itemsBuilder.addItem(item.getLongId(), itemView);
                }
            }
        }

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), config.widgetLayout);
        updateTitle(remoteViews, config);

        remoteViews.setTextViewText(R.id.subtitle,
                DateUtils.formatDateTime(mContext, System.currentTimeMillis(),
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME));
        remoteViews.setOnClickPendingIntent(R.id.button_refresh,
                createRefreshPendingIntent(appWidgetId));

        remoteViews.setPendingIntentTemplate(android.R.id.list,
                PendingIntent.getActivity(mContext, 0, new Intent(Intent.ACTION_VIEW)
                        .setPackage(mContext.getPackageName()),
                        PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        remoteViews.setRemoteAdapter(android.R.id.list, itemsBuilder.build());
        remoteViews.setEmptyView(android.R.id.list, R.id.empty);

        mAppWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        if (onComplete != null) {
             onComplete.run();
        }
    }

    private static void bindItem(Context context, RemoteViews remoteViews, Item item, int hotThreshold) {
        remoteViews.setTextViewText(R.id.title, item.getDisplayedTitle());
        remoteViews.setTextViewText(R.id.score, new SpannableStringBuilder()
                .append(getSpan(context, item.getScore(), "%1$dp", hotThreshold * AppUtils.HOT_FACTOR))
                .append(" - ")
                .append(getSpan(context, item.getKidCount(), "%1$dc", hotThreshold)));
        remoteViews.setOnClickFillInIntent(R.id.item_view, new Intent().setData(
                AppUtils.createItemUri(item.getId())));
    }

    private static SpannableString getSpan(Context context, int value, String format, int hotThreshold) {
        String text = String.format(Locale.US, format, value);
        SpannableString spannable = new SpannableString(text);
        if (value >= hotThreshold) {
            spannable.setSpan(new ForegroundColorSpan(
                    ContextCompat.getColor(context, R.color.orange500)),
                    0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    /**
     * Removes a widget and its configuration.
     *
     * @param appWidgetId the ID of the widget to remove
     */
    void remove(int appWidgetId) {
        cancelScheduledUpdate(appWidgetId);
        clearConfig(appWidgetId);
    }

    private void scheduleUpdate(int appWidgetId) {
        String frequency = getConfig(appWidgetId, R.string.pref_widget_frequency);
        long frequencyHourMillis = DateUtils.HOUR_IN_MILLIS
                * (TextUtils.isEmpty(frequency) ? DEFAULT_FREQUENCY_HOUR : Integer.valueOf(frequency));

        PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_APP_WIDGET_ID, appWidgetId);

        getJobScheduler().schedule(new JobInfo.Builder(appWidgetId,
                new ComponentName(mContext.getPackageName(), WidgetRefreshJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(extras)
                .setPeriodic(frequencyHourMillis)
                .build());
    }

    private void cancelScheduledUpdate(int appWidgetId) {
        getJobScheduler().cancel(appWidgetId);
    }

    private JobScheduler getJobScheduler() {
        return (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private String getConfig(int appWidgetId, @StringRes int key) {
        return mContext.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .getString(mContext.getString(key), null);
    }

    private void clearConfig(int appWidgetId) {
        mContext.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void updateTitle(RemoteViews remoteViews, WidgetConfig config) {
        remoteViews.setTextViewText(R.id.title, config.title);
        remoteViews.setOnClickPendingIntent(R.id.title,
                PendingIntent.getActivity(mContext, 0, config.customQuery ? new Intent(mContext, config.destination)
                        .putExtra(SearchManager.QUERY, config.title) : new Intent(mContext, config.destination),
                        PendingIntent.FLAG_IMMUTABLE));
    }

    @SuppressWarnings("deprecation")
    private void updateCollection(int appWidgetId, RemoteViews remoteViews, WidgetConfig config) {
        remoteViews.setTextViewText(R.id.subtitle,
                DateUtils.formatDateTime(mContext, System.currentTimeMillis(),
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME));
        remoteViews.setOnClickPendingIntent(R.id.button_refresh,
                createRefreshPendingIntent(appWidgetId));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Intent intent = new Intent(mContext, WidgetService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(WidgetService.EXTRA_CUSTOM_QUERY, config.customQuery)
                    .putExtra(WidgetService.EXTRA_SECTION, config.section)
                    .putExtra(WidgetService.EXTRA_LIGHT_THEME, config.isLightTheme);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setRemoteAdapter(android.R.id.list, intent);
            remoteViews.setEmptyView(android.R.id.list, R.id.empty);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        remoteViews.setPendingIntentTemplate(android.R.id.list,
                PendingIntent.getActivity(mContext, 0, new Intent(Intent.ACTION_VIEW)
                        .setPackage(mContext.getPackageName()),
                        flags));
    }

    private PendingIntent createRefreshPendingIntent(int appWidgetId) {
        return PendingIntent.getBroadcast(mContext, appWidgetId,
                new Intent(WidgetProvider.ACTION_REFRESH_WIDGET)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        .setPackage(mContext.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * A class that holds the configuration for a widget.
     */
    static class WidgetConfig {
        final boolean customQuery;
        final Class<? extends Activity> destination;
        final String title;
        final boolean isLightTheme;
        final @LayoutRes int widgetLayout;
        final String section;

        /**
         * Creates a new {@code WidgetConfig} based on the user's preferences.
         *
         * @param context the application context
         * @param theme   the widget theme
         * @param section the story section to display
         * @param query   a custom query for the stories
         * @return a new {@code WidgetConfig}
         */
        @NonNull
        static WidgetConfig createWidgetConfig(Context context, String theme, String section, String query) {
            int widgetLayout;
            boolean isLightTheme = false;
            if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_dark))) {
                widgetLayout = R.layout.appwidget_dark;
            } else if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_light))) {
                widgetLayout = R.layout.appwidget_light;
                isLightTheme = true;
            } else {
                widgetLayout = R.layout.appwidget;
            }
            String title;
            Class<? extends Activity> destination;
            if (!TextUtils.isEmpty(query)) {
                title = query;
                section = query;
                destination = SearchActivity.class;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_best))) {
                title = context.getString(R.string.title_activity_best);
                destination = BestActivity.class;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_top))) {
                title = context.getString(R.string.title_activity_list);
                destination = ListActivity.class;
            } else {
                // legacy "new stories" widget
                title = context.getString(R.string.title_activity_new);
                destination = NewActivity.class;
            }
            return new WidgetConfig(destination, title, section, isLightTheme, widgetLayout);
        }

        private WidgetConfig(Class<? extends Activity> destination, String title, String section,
                boolean isLightTheme, int widgetLayout) {
            this.destination = destination;
            this.title = title;
            this.section = section;
            this.isLightTheme = isLightTheme;
            this.widgetLayout = widgetLayout;
            this.customQuery = destination == SearchActivity.class;
        }
    }
}
