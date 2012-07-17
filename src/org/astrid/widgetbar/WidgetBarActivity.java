package org.astrid.widgetbar;

import org.astrid.widgetbar.appwidgethost.WidgetbarAppWidgetHost;
import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.model.WidgetbarAppWidgetInfo;
import org.astrid.widgetbar.model.WidgetbarModel;
import org.astrid.widgetbar.ui.CellLayout;
import org.astrid.widgetbar.ui.Widgetbar;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * WidgetBarActivity is the only activity of this application. This is where the
 * user may choose widgets to the widget bar and delete widgets from the
 * widgetbar.
 */
public class WidgetBarActivity extends Activity {
	private static final int REQUEST_PICK_APPWIDGET = 9;
	private static final int REQUEST_CREATE_APPWIDGET = 5;
	static final int APPWIDGET_HOST_ID = 1024;

	private AppWidgetManager mAppWidgetManager;
	private WidgetbarAppWidgetHost mAppWidgetHost;
	private LinearLayout mLayout;
	private Widgetbar mWidgetbar;

	private final int[] mCellCoordinates = new int[2];

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mWidgetbar = Widgetbar.getInstance();

		this.startService(new Intent(this, WidgetbarService.class));
		mAppWidgetManager = AppWidgetManager.getInstance(AppContext
				.getInstance().getContext());
		mAppWidgetHost = mWidgetbar.getAppWidgetHost();
		mAppWidgetHost.startListening();
		mLayout = (LinearLayout) findViewById(R.id.main_layout);
		Button selectButton = (Button) findViewById(R.id.select_button);
		selectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectWidget();
			}
		});
		((Button) findViewById(R.id.show_button))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						mWidgetbar.showWindow();
					}
				});
		((Button) findViewById(R.id.hide_button))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						mWidgetbar.hideWindow();
					}
				});
	}

	/**
	 * Add a widget to the workspace
	 * 
	 * Steps to add a new widget:
	 * <ol>
	 * <li>Calculate the grid spans needed to fit the widget</li>
	 * <li>Try to find the open space on the widget bar to fit this widget. If
	 * the space not found, return and the adding fails.</li>
	 * <li>Add the item(including the position and spans information calculated
	 * in the previous step) to the model(database)</li>
	 * <li>Create the view of the widget with AppWidgetHost</li>
	 * <li>Add the view to the current session</li>
	 * 
	 * @param data
	 *            The intent describing the appWidgetId
	 * @param cellInfo
	 *            The cell Information of current workspace, containing the
	 *            vacant cells (positions and spans) in particular
	 * 
	 */
	void addAppWidget(Intent data, CellLayout.CellInfo cellInfo,
			boolean insertAtFirst) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras
				.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);
		// calculate the grid spans needed to fit this widget
		CellLayout layout = (CellLayout) mWidgetbar.getWorkspace().getChildAt(
				cellInfo.session);
		int[] spans = layout.rectToCell(appWidgetInfo.minWidth,
				appWidgetInfo.minHeight);

		// Try to find a open space on the widgetbar to fit this widget
		final int[] xy = mCellCoordinates;
		if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
			Log.d("WidgetbarActivity", "Cannot find enough space");
			return;
		}

		Log.d("WidgetbarAcitivity", "Adding AppWidget of spans: (" + spans[0]
				+ ", " + spans[1] + "), in position (" + xy[0] + ", " + xy[1]
				+ ")");

		// Build Widgetbar-specific widget info and save to database
		WidgetbarAppWidgetInfo widgetbarInfo = new WidgetbarAppWidgetInfo(
				appWidgetId);
		widgetbarInfo.spanX = spans[0];
		widgetbarInfo.spanY = spans[1];

		WidgetbarModel.addItemToDatabase(this, widgetbarInfo, mWidgetbar
				.getWorkspace().getCurrentSession(), xy[0], xy[1], false);
		mWidgetbar.getModel().addWidgetbarAppWidget(widgetbarInfo);

		// Creates host View
		widgetbarInfo.hostView = mAppWidgetHost.createView(this, appWidgetId,
				appWidgetInfo);
		widgetbarInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
		widgetbarInfo.hostView.setTag(widgetbarInfo);
		// Add to workspace
		mWidgetbar.getWorkspace().addInCurrentSession(widgetbarInfo.hostView,
				xy[0], xy[1], widgetbarInfo.spanX, widgetbarInfo.spanY,
				insertAtFirst);

		mAppWidgetHost.startListening();
	}

	/**
	 * Find a empty slot for a specified size
	 * 
	 * @param cellInfo
	 *            Information for the cells
	 * @param xy
	 *            the parameter that will store the result
	 * @param spanX
	 *            the size of X,
	 * @param spanY
	 *            the size of Y
	 * @return true if successfully find a empty slot.
	 */
	private boolean findSlot(CellLayout.CellInfo cellInfo, int[] xy, int spanX,
			int spanY) {
		if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
			Log.d("WidgetbarActivity", "Finding on another session");
			cellInfo = mWidgetbar.getWorkspace().findAllVacantCells(null);
			if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
				Log.d("WidgetbarActivity", "Out of space");
				return false;
			}
		}
		return true;
	}

	/**
	 * Start a request_pick_appwidget activity. Allowing users to select a
	 * widget for the widget bar
	 */
	private void selectWidget() {
		// Allocate resources for a widget instance, it will return an ID for
		// that
		int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
		Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
		pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_PICK_APPWIDGET) {
				configureWidget(data);
			} else if (requestCode == REQUEST_CREATE_APPWIDGET) {
				addAppWidget(data, mWidgetbar.getWorkspace()
						.findAllVacantCells(null), true);
			}
		} else if (requestCode == RESULT_CANCELED && data != null) {
			int appWidgetId = data.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (appWidgetId == -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}

	/**
	 * @param data
	 *            The Intent containing information about the selected AppWidget
	 *            Check if the widget requires any configuration. If it
	 *            requires, the activity to configure The widget must be
	 *            launched
	 */

	private void configureWidget(Intent data) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras
				.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);
		if (appWidgetInfo.configure != null) {
			Intent intent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(appWidgetInfo.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
		} else {
			addAppWidget(data,
					mWidgetbar.getWorkspace().findAllVacantCells(null), true);
			// createWidget(data);
		}
	}

}
