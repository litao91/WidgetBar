package org.astrid.widgetbar;

import org.astrid.widgetbar.ui.DockView;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class widgetBarService extends Service {
	private DockView dockView;
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
		if(dockView==null) {
			dockView = new DockView();
		}
		this.dockView.show();
		return START_STICKY;
	}
}
