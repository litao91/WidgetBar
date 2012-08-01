package org.astrid.widgetbar.appwidgethost;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * Specific AppWidgetHost that creates our WidgetbarAppWidgetView.
 */
public class WidgetbarAppWidgetHost extends AppWidgetHost {
	public WidgetbarAppWidgetHost(Context context, int hostId) {
		super(context, hostId);
	}

	@Override
	protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
			AppWidgetProviderInfo appWidget) {
		return new WidgetbarAppWidgetHostView(context);
	}
}
