package org.astrid.widgetbar;

import org.astrid.widgetbar.ui.Widgetbar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class WidgetbarService extends Service {
	private Widgetbar mWidgetbar;
	static final int MSG_SHOW = 1;
	static final int MSG_HIDE = 2;

	/**
	 * Target we publish for clients to send message to IncomingHandler
	 */
	private final IWidgetbarService.Stub mBinder = new IWidgetbarService.Stub() {
		public void showWidgetbar(){
			Log.d("WigetbarService", "IPC calling show");
			Widgetbar.getInstance().safeShowWindow();
		}
		public void hideWidgetbar() {
			Log.d("WigetbarService", "IPC calling hide");
			Widgetbar.getInstance().safeHideWindow();
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	public static void sendCommand(Context paramContext, String commandStr) {
		Intent mIntent = new Intent(paramContext, WidgetbarService.class);
		mIntent.putExtra("command", commandStr);
		paramContext.startService(mIntent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mWidgetbar == null) {
			mWidgetbar = Widgetbar.getInstance();
		}
		new Handler().postDelayed(new Runnable() {
			public void run() {
				mWidgetbar.showWindow();
			}
		}, 1000);
		return START_STICKY;
	}
}
