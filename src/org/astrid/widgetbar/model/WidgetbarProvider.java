package org.astrid.widgetbar.model;

import java.util.ArrayList;

import org.astrid.widgetbar.ui.Widgetbar;

import android.appwidget.AppWidgetHost;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

public class WidgetbarProvider extends ContentProvider {
	private static final String DATABASE_NAME = "Widgetbar.db";
	private static final int DATABASE_VERSION = 1; 
	
	public static final String AUTHORITY = "org.astrid.widgetbar.settings";
	public static final String EXTRA_BIND_SOURCES = "org.astrid.widgetbar.settings.bindsources";
	public static final String EXTRA_BIND_TARGETS = "org.astrid.widgetbar.settings.bingdtargets";
	
	public static final String TABLE_FAVORITES = "favorites";
	public static final String PARAMETER_NOTIFY = "notify";
	
	/**
	 * Uri triggered at any registered ContentObserver when
	 * AppWidgetHost.deleteHost() is called during database creation.
	 * 
	 * Use this to recall AppWidgetHost#startListening() if needed
	 */
	static final Uri CONTENT_APPWIDGET_RESET_URI = 
			Uri.parse("content://" + AUTHORITY + "/appWidgetReset");
	private SQLiteOpenHelper mOpenHelper;
	
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		SqlArguments args = new SqlArguments(uri, null, null);
		if(TextUtils.isEmpty(args.where)) {
			return "vnd.android.cursor.dir/" + args.table;
		} else {
			return "vnd.android.cursor.item/" + args.table;
		}
	}
		
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, 
			String[] selectionArgs, String sortOrder) {
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(args.table);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
		result.setNotificationUri(getContext().getContentResolver(), uri);
		return result;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		SqlArguments args = new SqlArguments(uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = db.insert(args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;

	}
	
	@Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (db.insert(args.table, null, values[i]) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }


	private static class DatabaseHelper extends SQLiteOpenHelper {
		private final Context mContext;
		private final AppWidgetHost mAppWidgetHost;
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
			mAppWidgetHost = new AppWidgetHost(context, Widgetbar.APPWIDGET_HOST_ID);
		}
		/**
		 * Send notification that we've deleted the AppWidgetHost
		 * probably as part of the initial database creation. 
		 * The receiver may want to re-call AppWidgetHost#startListening() to 
		 * ensure callbacks are correctly set.
		 */
		private void sendAppWidgetResetNotify() {
			final ContentResolver resolver = mContext.getContentResolver();
			resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE favorites (" + 
					"_id INTEGER PRIMARY KEY,"+
					"intent TEXT," +
					"session INTEGER,"+
					"cellX INTEGER," +
					"cellY INTEGER,"+
					"spanX INTEGER,"+
					"spanY INTEGER," +
					"itemType INTEGER," +
					"appWidgetId INTEGER NOT NULL DEFAULT -1," +
					"uri TEXT" +
					");");
			//Database was just created, so wipe any previous widgets.
			if(mAppWidgetHost != null) {
				mAppWidgetHost.deleteHost();
				sendAppWidgetResetNotify();
			}
		}
		@Override 
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
			onCreate(db);
		}
	      /**
         * Launch the widget binder that walks through the Launcher database,
         * binding any matching widgets to the corresponding targets. We can't
         * bind ourselves because our parent process can't obtain the
         * BIND_APPWIDGET permission.
         */
        private void launchAppWidgetBinder(int[] bindSources, ArrayList<ComponentName> bindTargets) {
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                    "com.android.settings.LauncherAppWidgetBinder"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            final Bundle extras = new Bundle();
            extras.putIntArray(EXTRA_BIND_SOURCES, bindSources);
            extras.putParcelableArrayList(EXTRA_BIND_TARGETS, bindTargets);
            intent.putExtras(extras);

            mContext.startActivity(intent);
        }
        /**
         * Build a query string that will match any row where the column matches
         * anything in the values list.
         */
         static String buildOrWhereString(String column, int[] values) {
            StringBuilder selectWhere = new StringBuilder();
            for (int i = values.length - 1; i >= 0; i--) {
                selectWhere.append(column).append("=").append(values[i]);
                if (i > 0) {
                    selectWhere.append(" OR ");
                }
            }
            return selectWhere.toString();
        }
	}
	
	static class SqlArguments {
		public final String table;
		public final String where;
		public final String[] args;
		
		SqlArguments(Uri url, String where, String[] args) {
			if(url.getPathSegments().size() == 1) {
				this.table = url.getPathSegments().get(0);
				this.where = where;
				this.args = args;
			} else if (url.getPathSegments().size()!=2) {
				throw new IllegalArgumentException("Invalid URI:" + url);
			} else if (!TextUtils.isEmpty(where)) {
				throw new UnsupportedOperationException("WHERE clause not supported: " + url);
			} else {
				this.table = url.getPathSegments().get(0);
				this.where = "_id=" + ContentUris.parseId(url);
				this.args = null;
			}
		}
		SqlArguments(Uri url) {
			if(url.getPathSegments().size()==1) {
				table = url.getPathSegments().get(0);
				where = null;
				args = null;
			}else{
				throw new IllegalArgumentException("Invalid URI:" + url);
			}
		}
	}
}
