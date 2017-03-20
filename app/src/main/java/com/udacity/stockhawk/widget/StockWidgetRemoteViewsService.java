package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.DetailActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class StockWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;
            private DecimalFormat dollarFormatWithPlus;
            private DecimalFormat dollarFormat;
            private DecimalFormat percentageFormat;

            @Override
            public void onCreate() {
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");
                percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(Contract.Quote.URI,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}), null, null, Contract.Quote.COLUMN_SYMBOL);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(final int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                Context context = StockWidgetRemoteViewsService.this;
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item_quote);
                String symbol = data.getString(Contract.Quote.POSITION_SYMBOL);
                views.setTextViewText(R.id.symbol, data.getString(Contract.Quote.POSITION_SYMBOL));
                views.setTextViewText(R.id.price, dollarFormat.format(data.getFloat(Contract.Quote.POSITION_PRICE)));

                float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                int changeResId;
                if (rawAbsoluteChange < 0) {
                    changeResId = R.id.change_red;
                    views.setViewVisibility(R.id.change_red, View.VISIBLE);
                    views.setViewVisibility(R.id.change_green, View.GONE);
                } else {
                    changeResId = R.id.change_green;
                    views.setViewVisibility(R.id.change_green, View.VISIBLE);
                    views.setViewVisibility(R.id.change_red, View.GONE);
                }

                String change = dollarFormatWithPlus.format(rawAbsoluteChange);
                String percentage = percentageFormat.format(percentageChange / 100);

                if (PrefUtils.getDisplayMode(context)
                        .equals(context.getString(R.string.pref_display_mode_absolute_key))) {
                    views.setTextViewText(changeResId, change);
                } else {
                    views.setTextViewText(changeResId, percentage);
                }

                final Intent fillInIntent = new Intent()
                        .putExtra(DetailActivity.EXTRA_SYMBOL, symbol);
                views.setOnClickFillInIntent(R.id.stock_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item_quote);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(final int position) {
                if (data.moveToPosition(position)) {
                    return data.getLong(Contract.Quote.POSITION_ID);
                } else {
                    return position;
                }
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
