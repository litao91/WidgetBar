package org.astrid.widgetbar.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

import org.astrid.widgetbar.R;
import org.astrid.widgetbar.appwidgethost.WidgetbarAppWidgetHost;
import org.astrid.widgetbar.context.AppContext;
import org.astrid.widgetbar.model.ItemInfo;
import org.astrid.widgetbar.model.WidgetbarAppWidgetInfo;
import org.astrid.widgetbar.model.WidgetbarModel;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class Widgetbar{
    private static final int HIGH_DIP_STATUS_BAR_HEIGHT = 38; 
    private static final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
    private static final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
    
    static final int SESSION_COUNT = 3;
    static final int DEFAULT_SESSION = 1;
    static final int NUMBER_CELLS_X = 4;
    static final int NUMBER_CELLS_Y =4;
    
    private static final Object sLock = new Object();
    private static final WidgetbarModel sModel = new WidgetbarModel();
    private LayoutInflater mInflater;
    private boolean isShown = false;
    
    private DragLayer mDragLayer;
    private WidgetbarWorkspace mWorkspace;
    private AppWidgetManager mAppWidgetManager;
    
    public static final int APPWIDGET_HOST_ID = 1024;
    
    private int screenHeight;
    private int screenWidth;
    //Managers
    private ActivityManager mActivityManager;
    private WindowManager.LayoutParams mLayoutParams; 
    private PackageManager mPackageManager;
    private WindowManager windowManager;
    private WidgetbarAppWidgetHost mAppWidgetHost;
    
    
    private DisplayMetrics metrics = new DisplayMetrics();
    private static int mCurrentSession = DEFAULT_SESSION;
    
    private View mOverlayBarView;
    private static Widgetbar mInstance;
    
    private Widgetbar() {
        //super(AppContext.getInstance().getContext());
        mPackageManager =  getContext().getPackageManager();
        mActivityManager = ((ActivityManager)getContext().getSystemService("activity"));
        mInflater = (LayoutInflater)getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        startLoaders();
        mAppWidgetManager = AppWidgetManager.getInstance(getContext());
        mAppWidgetHost = new WidgetbarAppWidgetHost(getContext(), APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();
        mOverlayBarView = (DragLayer)mInflater.inflate(R.layout.widgetbar, null);
        setUpViews();
    }
    public static Widgetbar getInstance() {
        if(mInstance == null) {
            mInstance = new Widgetbar();
        }
        return mInstance;
    }
    
    private void setUpViews() {
        mDragLayer = (DragLayer)mOverlayBarView.findViewById(R.id.drag_layout);
        final DragLayer dragLayer = mDragLayer;
        mWorkspace = (WidgetbarWorkspace) dragLayer.findViewById(R.id.workspace);
        final WidgetbarWorkspace workspace = mWorkspace;
        dragLayer.setDragScoller(workspace);
    }
    
    private void bindAppWidgets(Widgetbar.WidgetbarBinder binder,
            LinkedList<WidgetbarAppWidgetInfo> appWidgets) {
        final WidgetbarWorkspace workspace = mWorkspace;
        if(!appWidgets.isEmpty()) {
            final WidgetbarAppWidgetInfo  item = appWidgets.removeFirst();
            final int appWidgetId = item.getAppWidgetId();
            final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            item.hostView = mAppWidgetHost.createView(getContext(), appWidgetId, appWidgetInfo);
            item.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            item.hostView.setTag(item);
            workspace.addInSession(item.hostView, item.session, item.cellX, item.cellY, item.spanX, item.spanY);
            workspace.requestLayout();
            
            binder.obtainMessage(WidgetbarBinder.MESSAGE_BIND_APPWIDGETS).sendToTarget();
        }
    }
    public WidgetbarWorkspace getWorkspace() {
        return mWorkspace;
    }
    public WidgetbarModel getModel() {
        return this.sModel;
    }
            
    /*
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
    
    
    
    protected void onDraw(Canvas mCanvas) {
        drawDock(mCanvas);
    }
    */
    
    public void show() {
    	if(!isShown) {
    		Log.d("WidgetBarView", "Showing");
        	this.windowManager = (WindowManager)mOverlayBarView.getContext().getApplicationContext().getSystemService("window");
        	this.windowManager.getDefaultDisplay().getMetrics(this.metrics);
        
        	Point outSize = new Point();
        	this.windowManager.getDefaultDisplay().getSize(outSize);
        	this.screenWidth = outSize.x;
        	this.screenHeight = outSize.y;
        	mLayoutParams = new WindowManager.LayoutParams(
        		screenWidth,screenHeight/2,
        		WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
        		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                	|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        	this.mLayoutParams.gravity = Gravity.BOTTOM;
        	this.windowManager.addView(mOverlayBarView, this.mLayoutParams);
        	isShown = true;
    	}
    }
    public static void setSession(int sessionNum) {
        synchronized(sLock) {
            mCurrentSession = sessionNum;
        }
    }
    public static int getSession() {
        synchronized(sLock) {
            return mCurrentSession;
        }
    }

    private void startLoaders() {
      sModel.loadWidgetbarItems(false, this);
    }
        

    public void onWidgetbarItemsLoded(ArrayList<ItemInfo> mWidgetbarItems,
            ArrayList<WidgetbarAppWidgetInfo> mWidgetbarAppWidgets) {
        // TODO Auto-generated method stub
        bindWidgetbarItems(mWidgetbarItems, mWidgetbarAppWidgets);
    }
    private Context getContext() {
        return AppContext.getInstance().getContext();
    }
    private void bindWidgetbarItems(ArrayList<ItemInfo> items,
            ArrayList<WidgetbarAppWidgetInfo> appWidgets) {
        final WidgetbarWorkspace workspace = mWorkspace;
    }
    
    private static class WidgetbarBinder extends Handler implements MessageQueue.IdleHandler {
        static final int MESSAGE_BIND_APPWIDGETS = 0x1;
        private final LinkedList<WidgetbarAppWidgetInfo> mAppWidgets;
        private final WeakReference<Widgetbar> mWidgetbar;
        
        WidgetbarBinder(Widgetbar widgetbar,
                ArrayList<WidgetbarAppWidgetInfo> appWidgets) {
            mWidgetbar = new WeakReference<Widgetbar>(widgetbar);
            
            //sort widgets so active workspace is bound first.
            final int currentSession = widgetbar.mWorkspace.getCurrentSession();
            final int size = appWidgets.size();
            mAppWidgets = new LinkedList<WidgetbarAppWidgetInfo>();
            
            for(int i = 0; i < size; i++) {
                WidgetbarAppWidgetInfo appWidgetInfo = appWidgets.get(i);
                if(appWidgetInfo.session == currentSession) {
                    mAppWidgets.addFirst(appWidgetInfo);
                } else {
                    mAppWidgets.addLast(appWidgetInfo);
                }
            }
        }
        public boolean queueIdle() {
            //Queue is idle, so start binding items
            startBindingAppWidgets();
            return false;
        }
        public void startBindingAppWidgets() {
            obtainMessage(MESSAGE_BIND_APPWIDGETS).sendToTarget();
        }
        @Override
        public void handleMessage(Message msg) {
            Widgetbar widgetbar = mWidgetbar.get();
            switch(msg.what) {
                case MESSAGE_BIND_APPWIDGETS: {
                    widgetbar.bindAppWidgets(this, mAppWidgets);
                    break;
                }
            }
        }
            
    }
}
