package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.ui.MainActivity;

public class StockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        if (QuoteSyncJob.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_detail);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

            views.setRemoteAdapter(R.id.widget_list,
                    new Intent(context, StockWidgetRemoteViewsService.class));

            Intent clickItemIntent = new Intent(context, DetailActivity.class);
            PendingIntent clickItemPendingIntent =
                    TaskStackBuilder.create(context)
                            .addNextIntentWithParentStack(clickItemIntent)
                            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_list, clickItemPendingIntent);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
}
