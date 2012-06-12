package org.astrid.widgetbar.ui;

import org.astrid.widgetbar.context.AppContext;

import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;
import android.view.WindowManager;

public class DockView extends View {
	private static final int HIGH_DIP_STATUS_BAR_HEIGHT = 38; 
	private static final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
	private static final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
	
	//Managers
	private ActivityManager activityManager;
	private WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams();
	private PackageManager packageManager;
	
	private Paint paint;
	private Paint PaintMinimized;
	private Paint paintVisible;
	
	private float currentX;
	private float currentY;
	private int visibleHeight;
	
	
	private Matrix resusableMatrix = new Matrix();
	public DockView() {
		super(AppContext.getInstance().getContext());
		this.mLayoutParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
		this.mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		
		this.packageManager =  AppContext.getInstance().getContext().getPackageManager();
		this.activityManager = ((ActivityManager)AppContext.getInstance().getContext().getSystemService("activity"));
	}
	
	private void drawDock(Canvas mCanvas) {
		
	}
}
