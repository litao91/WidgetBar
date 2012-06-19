package org.astrid.widgetbar;

import org.astrid.widgetbar.ui.Widgetbar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class widgetBarService extends Service {
	private Widgetbar widgetbarView;
	private static final int NOTIFICATION_ID = 34321266;

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
//		int i = super.onStartCommand(intent, flags, startId);
		Log.d("widgetBarService", "starting Command");
		if(widgetbarView==null) {
			widgetbarView = new Widgetbar();
		}
		this.widgetbarView.show();
		return START_STICKY;
	}
}
