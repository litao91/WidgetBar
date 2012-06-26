package org.astrid.widgetbar.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.ui.Widgetbar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

/**
 * Maintain in-memory state of the Launcher. It is expected that there should be only
 * one WidgetbarModel object held in a static. 
 * Also Provide APIs for updating the database state
 * for the WidgetBar
 */
public class WidgetbarModel {
	private static final int APPLICATION_NOT_RESPONDING_TIMEOUT = 5000;
	
	private Thread mWidgetbarLoaderThread;
	private WidgetbarItemsLoader mWidgetbarItemsLoader;
	
	private ArrayList<WidgetbarAppWidgetInfo> mWidgetbarAppWidgets;
	private ArrayList<ItemInfo> mWidgetbarItems;
	private boolean mWidgetbarItemsLoaded;
	synchronized void abortLoaders() {
		if(mWidgetbarItemsLoader!=null) {
			mWidgetbarItemsLoader.stop();
			mWidgetbarItemsLoaded = false;
		}
	}
	/**
	 * Load all of the items on the bar, currently widgets only
	 */
	public void loadWidgetbarItems(boolean isLaunching, Widgetbar widgetbar) {
		if(isLaunching && isWidgetbarLoaded()) {
			widgetbar.onWidgetbarItemsLoded(mWidgetbarItems, mWidgetbarAppWidgets);
			return;
		}
		if(mWidgetbarItemsLoader!= null && mWidgetbarItemsLoader.isRunning()) {
			mWidgetbarItemsLoader.stop();
			//Wait for the currently running thread to finish, this can take 
			// a little time but it should be well below the timeout limit.
			try {
				mWidgetbarLoaderThread.join(APPLICATION_NOT_RESPONDING_TIMEOUT);
			} catch (InterruptedException e) {
				// Nothing to do basically
			}
		}
		mWidgetbarItemsLoaded = false;
		mWidgetbarItemsLoader = new WidgetbarItemsLoader(widgetbar);
		mWidgetbarLoaderThread = new Thread(mWidgetbarItemsLoader, "Widgetbar Items Loader");
		mWidgetbarLoaderThread.start();
	}
	
	boolean isWidgetbarLoaded() {
		return mWidgetbarItems != null && mWidgetbarAppWidgets != null && mWidgetbarItemsLoaded;
	}
	private static final AtomicInteger sWorkspaceLoaderCount = new AtomicInteger(1);
	
	private class WidgetbarItemsLoader implements Runnable {
		private volatile boolean mStopped;
		private volatile boolean mRunning;
		
		private final WeakReference<Widgetbar> mWidgetbar;
		private final int mId;
		
		public WidgetbarItemsLoader(Widgetbar widgetbar){
			mWidgetbar = new WeakReference<Widgetbar>(widgetbar);
			mId = sWorkspaceLoaderCount.getAndIncrement();
		}
		public void stop() {
			mStopped = true;
		}
		public boolean isRunning() {
			return mRunning;
		}
		public void run() {
			// TODO Auto-generated method stub
			mRunning = true;
			android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
			final ContentResolver contentResolver = AppContext.getInstance().getContext().getContentResolver();
			mWidgetbarAppWidgets = new ArrayList<WidgetbarAppWidgetInfo>();
			mWidgetbarItems = new ArrayList<ItemInfo>();
			Widgetbar widgetbar = mWidgetbar.get();
			
			final ArrayList<ItemInfo> widgetbarItems = mWidgetbarItems;
			final ArrayList<WidgetbarAppWidgetInfo> widgetbarAppWidgets = mWidgetbarAppWidgets;
			
			final Cursor c =  contentResolver.query(
					WidgetbarSettings.Favoriates.CONTENT_URI, null, null, null,null);
			try{
				final int idIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates._ID);
				final int intentIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.INTENT);
				final int appWidgetIdIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.APPWIDGET_ID);
				final int sessionIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.SESSION);
				final int cellXIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.CELLX); 
				final int cellYIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.CELLY);
				final int spanXIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.SPANX);
				final int spanYIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.SPANX);
				final int uriIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.URI);
				WidgetbarAppWidgetInfo appWidgetInfo;
				while(!mStopped && c.moveToNext()) {
					//Need switch and cases if we have multiple item types
					//But currently, widget only
					int appWidgetId = c.getInt(appWidgetIdIndex);
					appWidgetInfo = new WidgetbarAppWidgetInfo(appWidgetId);
					appWidgetInfo.id = c.getLong(idIndex);
					appWidgetInfo.session = c.getInt(sessionIndex);
					appWidgetInfo.cellX = c.getInt(cellXIndex);
					appWidgetInfo.cellY = c.getInt(cellYIndex);
					appWidgetInfo.spanX = c.getInt(spanXIndex);
					appWidgetInfo.spanY = c.getInt(spanYIndex);
					mWidgetbarAppWidgets.add(appWidgetInfo);
				}
			} catch (Exception e) {
				Log.w("WidgetbarModel", "Items loading interrupted:", e);
			} finally {
				c.close();
			}
			if(!mStopped) {
				//Create a copy of the lists in case the workspace loader is restarted
				//And the list are cleared before the UI can go through them
				final ArrayList<ItemInfo> uiWidgetbarItems = new ArrayList<ItemInfo>(widgetbarItems);
				final ArrayList<WidgetbarAppWidgetInfo> uiWidgetbarWidgets = 
						new ArrayList<WidgetbarAppWidgetInfo>(widgetbarAppWidgets);
				widgetbar.onWidgetbarItemsLoded(uiWidgetbarItems, uiWidgetbarWidgets);
			}
			mRunning = false;
		}
	}
	/**
	 * Remove any WidgetbarAppWigetHostView references in our widgets.
	 */
	private void unbindAppWidgetHostViews(ArrayList<WidgetbarAppWidgetInfo> appWidgets) {
		if(appWidgets != null) {
			final int count = appWidgets.size();
			for(int i=0; i < count; i++) {
				WidgetbarAppWidgetInfo widgetbarInfo = appWidgets.get(i);
				widgetbarInfo.hostView = null;
			}
			
		}
	}
	/**
	 * Add a widget to the bar
	 */
	public void addWidgetbarAppWidget(WidgetbarAppWidgetInfo info) {
		mWidgetbarAppWidgets.add(info);
	}
	/**
	 * Remove a widget from the bar
	 */
	void removeWidgetbarAppWidget(WidgetbarAppWidgetInfo info) {
		mWidgetbarAppWidgets.remove(info);
	}
	
	/**
	 * Add an item to the database
	 */
	public static void addItemToDatabase(Context context, ItemInfo item, int session, int cellX, int cellY, boolean notify) {
		item.session = session;
		item.cellX = cellX;
		item.cellY = cellY;
		final ContentValues values = new ContentValues();
		final ContentResolver cr = context.getContentResolver();
		Log.d("WidgetbarModel", "Resolver is "+cr);
		item.onAddToDatabase(values);
		Uri result = cr.insert(notify?WidgetbarSettings.Favoriates.CONTENT_URI:
			WidgetbarSettings.Favoriates.CONTENT_URI_NO_NOTIFICATION, values);
		if(result!=null){
			item.id = Integer.parseInt(result.getPathSegments().get(1));
		}
	}
	/**
	 * update an item to the database
	 */ 
	static void updateItemInDatabase(Context context, ItemInfo item) {
		final ContentValues  values = new ContentValues();
		final ContentResolver cr = context.getContentResolver();
		item.onAddToDatabase(values);
		cr.update(WidgetbarSettings.Favoriates.getContentUri(item.id,false), values, null, null);
	}
	/**
	 * Remove the specified item from the database
	 */
	static void deleteItemFromDatabase(Context context, ItemInfo item) {
		final ContentResolver cr = context.getContentResolver();
		cr.delete(WidgetbarSettings.Favoriates.getContentUri(item.id, false), null, null);
	}
	
}
