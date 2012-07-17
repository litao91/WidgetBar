package org.astrid.widgetbar;

import org.astrid.widgetbar.context.AppContext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.d("Receiver", "onReceive");
		if (!AppContext.getInstance().isInitialized()) {
			AppContext.getInstance().init(context);
		}
		context.startService(new Intent(context, WidgetbarService.class));
	}

}
