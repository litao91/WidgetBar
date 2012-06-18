package org.astrid.widgetbar.appwidgethost;

import android.appwidget.AppWidgetHostView;
import android.content.ContentValues;


public class WidgetBarAppWidgetInfo extends ItemInfo {
	/**
	 * Identifier for this widget when talking with AppWidgetManager for updates
	 */
	int appWidgetId;
	/**
	 * View that holds this widget after it's been created. This view isn't 
	 * created until WidgetBar knows it's needed.
	 */
	AppWidgetHostView hostView = null;
	WidgetBarAppWidgetInfo(int appWidgetId) {
		itemType = WidgetBarSettings.Favoriates.ITEM_TYPE_APPWIDGE;
		this.appWidgetId = appWidgetId;
	}
	@Override
	void onAddToDatabase(ContentValues values) {
		super.onAddToDatabase(values);
		values.put(WidgetBarSettings.Favoriates.APPWIDGET_ID, appWidgetId);
	}
	@Override
	public String toString() {
		return Integer.toString(appWidgetId);
	}
}
