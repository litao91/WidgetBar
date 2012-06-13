package org.astrid.widgetbar.ui;

import org.astrid.widgetbar.context.AppContext;

import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

public class DockView extends View {
	private static final int HIGH_DIP_STATUS_BAR_HEIGHT = 38; 
	private static final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
	private static final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
	
	private int screenHeight;
	private int screenWidth;
	
	//Managers
	private ActivityManager activityManager;
	private WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams();
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
	public DockView() {
		super(AppContext.getInstance().getContext());
		this.mLayoutParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
		this.mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		this.mLayoutParams.gravity = 48;
		
		this.packageManager =  AppContext.getInstance().getContext().getPackageManager();
		this.activityManager = ((ActivityManager)AppContext.getInstance().getContext().getSystemService("activity"));
	}
	
	private void drawDock(Canvas mCanvas) {
		//create a path
		Path testPath = new Path();
		testPath.moveTo((float)screenHeight,0.0F);
		testPath.lineTo((float)screenHeight, (float)screenWidth);
		testPath.lineTo((float)screenHeight, (float)screenWidth);
		testPath.close();
		if(this.paintMinimized == null) {
			paintMinimized = new Paint();
			paintMinimized.setAntiAlias(true);
			paintMinimized.setStyle(Paint.Style.FILL);
			paintMinimized.setColor(0);
		}
		mCanvas.drawPath(testPath, paintMinimized);
		
	}
	
	
	@Override
	protected void onDraw(Canvas mCanvas) {
		drawDock(mCanvas);
	}
	
	public void show() {
		this.windowManager = ((WindowManager)super.getContext().getApplicationContext().getSystemService("window"));
		this.windowManager.getDefaultDisplay().getMetrics(this.metrics);
		this.windowManager.addView(this, this.mLayoutParams);
		
		Point outSize = new Point();
		this.windowManager.getDefaultDisplay().getSize(outSize);
		this.screenWidth = outSize.x;
		this.screenHeight = outSize.y;
		
	}
	
}
