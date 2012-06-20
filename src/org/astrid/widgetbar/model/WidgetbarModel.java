package org.astrid.widgetbar.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.ui.Widgetbar;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Process;
import android.util.Log;

/**
 * Maintain in-memory state of the Launcher. It is expected that there should be only
 * one WidgetbarModel object held in a static. 
 * Also Provide APIs for updating the database state
 * for the WidgetBar
 */
public class WidgetbarModel {
	private boolean mApplicationLoaded;
	private boolean mBarItemsloaded;
	
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
	void loadWidgetbarItems(boolean isLaunching, Widgetbar widgetbar) {
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
			//If the thread we are intrrupting was tasked to load the list of
			// applications make sure we keep that information in the new loader
			//spwaned below
			//
		}
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
				final int sessionIndex = c.getColumnIndexOrThrow(WidgetbarSettings.Favoriates.SPACE_SESSION);
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
				//Note: better to pot it on UI thread
				widgetbar.onItemsLoaded(uiWidgetbarItems, uiWidgetbarWidgets);
			}
			mRunning = false;
		}
	}
	
}
