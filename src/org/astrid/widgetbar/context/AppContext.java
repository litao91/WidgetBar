package org.astrid.widgetbar.context;

import android.content.Context;
import android.util.Log;

public class AppContext {
	private static AppContext mInstance = new AppContext();
	private Context context;
	public static AppContext getInstance() {
		return mInstance;
	}
	public Context getContext() {
		return this.context;
	}
	public void init(Context context) {
		Log.d("AppContext","Initializing context "+context);
		this.context = context;
	}
	public boolean isInitialized() {
		if(this.context!=null){
			return true;
		}else {
			return false;
		}
	}
}
