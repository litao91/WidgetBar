package org.astrid.widgetbar.appwidgethost;

import android.provider.BaseColumns;


public class WidgetBarSettings {
	public static interface BaseBarColumns extends BaseColumns {
		public static final String TITLE = "title";
		public static final String ITEM_TYPE = "itemType";
		public static final int ITEM_TYPE_APPLICATION = 0;
		public static final int ITEM_TYPE_SHORTCUT = 1;
		
	}
	public static final class Favoriates implements BaseBarColumns {
		public static final String SPACE_SESSION = "space_session";
		public static final String CELLX = "cellX";
		public static final String CELLY = "cellY";
		public static final String SPANX = "spanX";
		public static final String SPANY = "spanY";
		public static final int ITEM_TYPE_USER_FOLDER = 2;
		public static final int ITEM_TYPE_LIVE_FOLTER = 3;
		public static final int ITEM_TYPE_APPWIDGE= 4;
		public static final String APPWIDGET_ID = "appWidgetId";
	}
}

