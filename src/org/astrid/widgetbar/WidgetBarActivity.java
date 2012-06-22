package org.astrid.widgetbar;


import org.astrid.widgetbar.appwidgethost.WidgetbarAppWidgetHost;
import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.model.WidgetbarAppWidgetInfo;
import org.astrid.widgetbar.model.WidgetbarModel;
import org.astrid.widgetbar.ui.CellLayout;
import org.astrid.widgetbar.ui.Widgetbar;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
	private Widgetbar mWidgetbar;
	private CellLayout.CellInfo mAddItemCellInfo;
	
	private final int[] mCellCoordinates = new int[2];
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mWidgetbar = Widgetbar.getInstance();
        
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
    void addAppWidget(Intent data, CellLayout.CellInfo cellInfo, boolean insertAtFirst) {
    	Bundle extras = data.getExtras();
    	int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1); 
    	
    	AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
    	//calculate the grid spans needed to fit this widget
    	CellLayout layout = (CellLayout) mWidgetbar.getWorkspace().getChildAt(cellInfo.session);
    	int[] spans = layout.rectToCell(appWidgetInfo.minWidth, appWidgetInfo.minHeight);
    	
    	//Try finding the open space on the widgetbar to fit this widget
    	final int[] xy = mCellCoordinates;
    	if(!findSlot(cellInfo, xy, spans[0], spans[1])) return;
    	
    	//Build Widgetbar-specific widget info and save to database
    	WidgetbarAppWidgetInfo widgetbarInfo = new WidgetbarAppWidgetInfo(appWidgetId);
    	widgetbarInfo.spanX = spans[0];
    	widgetbarInfo.spanY = spans[1];
    	
    	WidgetbarModel.addItemToDatabase(this, widgetbarInfo,
    			mWidgetbar.getWorkspace().getCurrentSession(),
    			xy[0], xy[1], false);
    	mWidgetbar.getModel().addWidgetbarAppWidget(widgetbarInfo);
    	widgetbarInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
    	widgetbarInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
    	widgetbarInfo.hostView.setTag(widgetbarInfo);
    	mWidgetbar.getWorkspace().addInCurrentSession(widgetbarInfo.hostView, 
    			xy[0], xy[1], widgetbarInfo.spanX, widgetbarInfo.spanY, insertAtFirst);
    }
    	
    private boolean findSlot(CellLayout.CellInfo cellInfo, int[] xy, int spanX, int spanY) {
    	if(!cellInfo.findCellForSpan(xy, spanX, spanY)) {
    		cellInfo = mWidgetbar.getWorkspace().findAllVacantCells(null);
    		if(!cellInfo.findCellForSpan(xy, spanX, spanY)) {
    			return false;
    		}
    	}
    	return true;
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
				if(mAddItemCellInfo==null) {
					mAddItemCellInfo = new CellLayout.CellInfo();
				}
				addAppWidget(data, mAddItemCellInfo,true );
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
			if(mAddItemCellInfo==null) {
				mAddItemCellInfo = new CellLayout.CellInfo();
			}
			addAppWidget(data, mAddItemCellInfo,true );
			//createWidget(data);
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