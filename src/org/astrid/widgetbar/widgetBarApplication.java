package org.astrid.widgetbar;

import org.astrid.widgetbar.context.AppContext;

import android.app.Application;

public class widgetBarApplication extends Application {

	public void onCreate() {
		super.onCreate();
		AppContext.getInstance().init(this);
	}
}
