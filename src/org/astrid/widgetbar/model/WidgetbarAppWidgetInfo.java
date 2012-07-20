package org.astrid.widgetbar.model;

import android.appwidget.AppWidgetHostView;
import android.content.ContentValues;

public class WidgetbarAppWidgetInfo extends ItemInfo {
	/**
	 * Identifier for this widget when talking with AppWidgetManager for updates
	 */
	int appWidgetId;
	/**
	 * View that holds this widget after it's been created. This view isn't
	 * created until WidgetBar knows it's needed.
	 */
	public AppWidgetHostView hostView = null;

	public WidgetbarAppWidgetInfo(int appWidgetId) {
		itemType = WidgetbarSettings.Favoriates.ITEM_TYPE_APPWIDGE;
		this.appWidgetId = appWidgetId;
	}

	@Override
	void onAddToDatabase(ContentValues values) {
		super.onAddToDatabase(values);
		values.put(WidgetbarSettings.Favoriates.APPWIDGET_ID, appWidgetId);
	}

	@Override
	public String toString() {
		return Integer.toString(appWidgetId);
	}

	public int getAppWidgetId() {
		return appWidgetId;
	}
}
