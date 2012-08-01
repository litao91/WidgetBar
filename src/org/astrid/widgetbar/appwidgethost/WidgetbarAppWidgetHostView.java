package org.astrid.widgetbar.appwidgethost;

import org.astrid.widgetbar.R;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

/**
 * The view class of the appwidget. We need to handle the long press event here
 * if we want to implement drag and drop or override other non-default events.
 * 
 */
public class WidgetbarAppWidgetHostView extends AppWidgetHostView {
	private LayoutInflater mInflater;

	public WidgetbarAppWidgetHostView(Context context) {
		super(context);
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	/**
	 * Show error view when the appwidgetview isn't generated properly
	 */
	@Override
	protected View getErrorView() {
		return mInflater.inflate(R.layout.appwidget_error, this, false);

	}

	/**
	 * Override this to prepare for the drag and drop if you need to. Watch for
	 * longpress events at this level to make sure users can always pick up this
	 * widget. Not implemented yet
	 */
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return super.onInterceptTouchEvent(ev);
	}
}
