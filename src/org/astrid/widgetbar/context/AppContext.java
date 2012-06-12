package org.astrid.widgetbar.context;

import android.content.Context;

public class AppContext {
	private static AppContext mInstance = new AppContext();
	private Context context;
	public static AppContext getInstance() {
		return mInstance;
	}
	public Context getContext() {
		return this.context;
	}
	public boolean isInitialized() {
		if(this.context!=null){
			return true;
		}else {
			return false;
		}
	}
}
