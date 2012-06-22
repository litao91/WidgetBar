package org.astrid.widgetbar.model;


import android.provider.BaseColumns;
import android.net.Uri;


public class WidgetbarSettings {
	public static interface BaseBarColumns extends BaseColumns {
		public static final String TITLE = "title";
		public static final String INTENT = "intent";
		public static final String ITEM_TYPE = "itemType";
		public static final int ITEM_TYPE_APPLICATION = 0;
		public static final int ITEM_TYPE_SHORTCUT = 1;
		
	}
	public static final class Favoriates implements BaseBarColumns {
		/**
		 * The content:// style URL fo this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://"+WidgetbarProvider.AUTHORITY + "/" + WidgetbarProvider.TABLE_FAVORITES +
				"?" + WidgetbarProvider.PARAMETER_NOTIFY + "=true");
		static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://"+WidgetbarProvider.AUTHORITY + "/" + WidgetbarProvider.TABLE_FAVORITES +
				"?" + WidgetbarProvider.PARAMETER_NOTIFY + "=false");
		public static final String SPACE_SESSION = "space_session";
		public static final String CELLX = "cellX";
		public static final String CELLY = "cellY";
		public static final String SPANX = "spanX";
		public static final String SPANY = "spanY";
		public static final String URI = "uri";
		public static final int ITEM_TYPE_USER_FOLDER = 2;
		public static final int ITEM_TYPE_LIVE_FOLTER = 3;
		public static final int ITEM_TYPE_APPWIDGE= 4;
		public static final String APPWIDGET_ID = "appWidgetId";
		/**
		 * The content:// style URL for a given row, identified by it's id.
		 * @param id the row id
		 * @param notify True to send a notification is the content changes
		 * @return The unique content URL for the specified row.
		 */
		static Uri getContentUri(long id, boolean notify) {
			return Uri.parse("content://"+WidgetbarProvider.AUTHORITY + 
					"/" + WidgetbarProvider.TABLE_FAVORITES + "/" + id + "?" +
					WidgetbarProvider.PARAMETER_NOTIFY + "=" + notify);
						
		}
	}
}

