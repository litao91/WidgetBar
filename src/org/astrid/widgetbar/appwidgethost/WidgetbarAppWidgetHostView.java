package org.astrid.widgetbar.appwidgethost;


import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;


public class WidgetbarAppWidgetHostView extends AppWidgetHostView {
	private LayoutInflater mInflater;
	public WidgetbarAppWidgetHostView(Context context) {
		super(context);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
}
