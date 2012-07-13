package org.astrid.widgetbar;

import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.ui.Widgetbar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class widgetBarService extends Service {
	private Widgetbar mWidgetbar;

	@Override
	public void onCreate(){
		super.onCreate();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void sendCommand(Context paramContext, String commandStr) {
		Intent mIntent = new Intent(paramContext, widgetBarService.class);
		mIntent.putExtra("command", commandStr);
		paramContext.startService(mIntent);
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(mWidgetbar==null) {
			mWidgetbar = Widgetbar.getInstance();
		}
		new Handler().postDelayed(new Runnable(){
			public void run() {
				mWidgetbar.showWindow();
			}
		}, 1000);
		return START_STICKY;
	}
}
