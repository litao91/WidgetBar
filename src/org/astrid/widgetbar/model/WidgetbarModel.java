package org.astrid.widgetbar.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.astrid.widgetbar.appwidgethost.ItemInfo;
import org.astrid.widgetbar.appwidgethost.WidgetbarAppWidgetInfo;
import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.ui.Widgetbar;

import android.content.ContentResolver;
import android.os.Process;

/**
 * Maintain in-memory state of the Launcher. It is expected that there should be only
 * one WidgetbarModel object held in a static. 
 * Also Provide APIs for updating the database state
 * for the WidgetBar
 */
public class WidgetbarModel {
	private boolean mApplicationLoaded;
	private boolean mBarItemsloaded;
	
	private Thread mApplicationLoaderThread;
	
	private ArrayList<WidgetbarAppWidgetInfo> mWdigetbarAppWidgets;
	private ArrayList<ItemInfo> mWidgetbarItems;
	/**
	 * Load all of the items on the bar
	 */
		
	private static final AtomicInteger sWorkspaceLoaderCount = new AtomicInteger(1);
	private class WidgetbarItemsLoader implements Runnable {
		private volatile boolean mStopped;
		private volatile boolean mRunning;
		
		private final WeakReference<Widgetbar> mWidgetbar;
		private final int mId;
		WidgetbarItemsLoader(Widgetbar widgetbar){
			mWidgetbar = new WeakReference<Widgetbar>(widgetbar);
			mId = sWorkspaceLoaderCount.getAndIncrement();
		}
		void stop() {
			mStopped = true;
		}
		boolean isRunning() {
			return mRunning;
		}
		public void run() {
			// TODO Auto-generated method stub
			mRunning = true;
			android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
			final ContentResolver contentResolver = AppContext.getInstance().getContext().getContentResolver();
			mWdigetbarAppWidgets = new ArrayList<WidgetbarAppWidgetInfo>();
			mWidgetbarItems = new ArrayList<ItemInfo>();
			
			final ArrayList<ItemInfo> widgetbarItems = mWidgetbarItems;
			final ArrayList<WidgetbarAppWidgetInfo> widgetbarAppWidgets = mWidgetBarAppWidgets;
			
			fianl Cursor c =  contentResolver.query(
					WidgetbarSettings.
			
			
			
			
			
		}
	
	}
	
	
}
