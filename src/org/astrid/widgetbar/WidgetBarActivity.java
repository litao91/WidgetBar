package org.astrid.widgetbar;


import org.astrid.widgetbar.appwidgethost.WidgetbarAppWidgetHost;
import org.astrid.widgetbar.context.AppContext;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class WidgetBarActivity extends Activity {
	private static final int REQUEST_PICK_APPWIDGET = 9;
	private static final int REQUEST_CREATE_APPWIDGET = 5;
	static final int APPWIDGET_HOST_ID = 1024;
	
	private AppWidgetManager mAppWidgetManager;
	private WidgetbarAppWidgetHost mAppWidgetHost;
	private LinearLayout mLayout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.startService(new Intent(this, widgetBarService.class));
		mAppWidgetManager = AppWidgetManager.getInstance(AppContext.getInstance().getContext());
		mAppWidgetHost = new WidgetbarAppWidgetHost(AppContext.getInstance().getContext(), APPWIDGET_HOST_ID);
		mLayout = (LinearLayout)findViewById(R.id.main_layout);
		Button selectButton = (Button)findViewById(R.id.select_button);
		selectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectWidget();
			}
		});
    }
    /**
     * Start an activity to let user select which widget he wants to add to the app.
     */
	private void selectWidget(){
		//Allocate resources for a widget instance, it will return an ID for that
		int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
		Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
		pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK) {
			if(requestCode == REQUEST_PICK_APPWIDGET) {
				configureWidget(data);
			}else if(requestCode == REQUEST_CREATE_APPWIDGET){
				createWidget(data);
			}
		} else if(requestCode == RESULT_CANCELED && data != null) {
			int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if(appWidgetId == -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}
	
	/**
	 * @param data The Intent containing information about the selected AppWidget
	 * Check if the widget requires any configuration. If it requires, the activity to configure
	 * The widget must be launched
	 */
	
	private void configureWidget(Intent data) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
		if(appWidgetInfo.configure != null) {
			Intent intent = 
					new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
		}else {
			createWidget(data);
		}
	}
	/**
	 * @param data The selected widget info
	 * Create the widget itself. Use the Widget ID and AppWidgetProviderInfo to ask to the
	 * AppWidgetHost for creating a view
	 * 
	 * It will return an AppWidgetHostView which is a derived class from the View
	 * 
	 * Note that this simple version is just temporarily used for test only 
	 */
	private void createWidget(Intent data) {
		Bundle extras= data.getExtras();
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidgetInfo = 
				mAppWidgetManager.getAppWidgetInfo(appWidgetId);
		AppWidgetHostView hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
		hostView.setAppWidget(appWidgetId, appWidgetInfo);
		mLayout.addView(hostView);
	}
}