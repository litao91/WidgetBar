package org.astrid.widgetbar.ui;

import java.util.ArrayList;

import org.astrid.widgetbar.appwidgethost.WidgetbarAppWidgetHost;
import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.model.ItemInfo;
import org.astrid.widgetbar.model.WidgetbarAppWidgetInfo;

import android.R;
import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class Widgetbar extends View {
	private static final int HIGH_DIP_STATUS_BAR_HEIGHT = 38; 
	private static final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
	private static final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
	
	public static final int APPWIDGET_HOST_ID = 1024;
	
	private int screenHeight;
	private int screenWidth;
	
	//Managers
	private ActivityManager activityManager;
	private WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams(
			150,50,
			WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
		    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
		    	|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
		    PixelFormat.TRANSLUCENT);
	private PackageManager packageManager;
	private WindowManager windowManager;
	
	private Paint paint;
	private Paint paintMinimized;
	private Paint paintVisible;
	
	private float currentX;
	private float currentY;
	private int visibleHeight;
	private Matrix resusableMatrix = new Matrix();
	private DisplayMetrics metrics = new DisplayMetrics();
	
	public Widgetbar() {
		super(AppContext.getInstance().getContext());
		
		this.packageManager =  AppContext.getInstance().getContext().getPackageManager();
		this.activityManager = ((ActivityManager)AppContext.getInstance().getContext().getSystemService("activity"));
	}
	
	private void drawDock(Canvas mCanvas) {
		//create a path
		Log.d("WidgetBarView", "drawing");
		Path testPath = new Path();
		testPath.moveTo((float)screenHeight,0.0F);
		testPath.lineTo((float)screenHeight, (float)screenWidth);
		testPath.close();
		if(this.paintMinimized == null) {
			paintMinimized = new Paint();
			paintMinimized.setAntiAlias(true);
			paintMinimized.setStyle(Paint.Style.FILL);
			paintMinimized.setARGB(100,100,100,100);
		}
		//mCanvas.drawPath(testPath, paintMinimized);
		mCanvas.drawRect(0,0,150,50,paintMinimized);
	}
	
	
	
	@Override
	protected void onDraw(Canvas mCanvas) {
		drawDock(mCanvas);
	}
	
	public void show() {
		Log.d("WidgetBarView", "Showing");
		this.windowManager = ((WindowManager)super.getContext().getApplicationContext().getSystemService("window"));
		this.windowManager.getDefaultDisplay().getMetrics(this.metrics);
		
		Point outSize = new Point();
		this.windowManager.getDefaultDisplay().getSize(outSize);
		this.screenWidth = outSize.x;
		this.screenHeight = outSize.y;
		this.mLayoutParams.gravity = Gravity.BOTTOM;
		this.windowManager.addView(this, this.mLayoutParams);
	}
	public static void setSession(int sessionNum) {
		
	}
	public void onItemsLoaded(ArrayList<ItemInfo> uiWidgetbarItems,
			ArrayList<WidgetbarAppWidgetInfo> uiWidgetbarWidgets) {
		// TODO Auto-generated method stub
	}

	public void onWidgetbarItemsLoded(ArrayList<ItemInfo> mWidgetbarItems,
			ArrayList<WidgetbarAppWidgetInfo> mWidgetbarAppWidgets) {
		// TODO Auto-generated method stub
		
	}
	
}
