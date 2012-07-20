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
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Manage the views, work closely with the model, prepare and manage
 * the views of the of the widgetbar, work as a controller for the
 * the user interface.
 */
public class Widgetbar {
    private static final String TAG = "Widgetbar";
    private static final boolean DEBUG = true;

    static final int SESSION_COUNT = 3;
    static final int DEFAULT_SESSION = 1;
    static final int NUMBER_CELLS_X = 4;
    static final int NUMBER_CELLS_Y = 3;

    private static final Object sLock = new Object();
    private static final WidgetbarModel sModel = new WidgetbarModel();
    private static WidgetbarWorkspace mWorkspace;
    private LayoutInflater mInflater;
    private DragLayer mDragLayer;
    private AppWidgetManager mAppWidgetManager;

    private WidgetbarWindow mWindow;

    private WidgetbarBinder mBinder;

    public static final int APPWIDGET_HOST_ID = 1024;

    // flags
    boolean mInitialized;
    boolean mWindowCreated;
    boolean mWindowAdded;
    boolean mWindowShown;
    boolean mInShowWindow;
    boolean mItemsLoaded;

    // Sizes
    private int screenHeight;
    private int screenWidth;
    private int barHeight;
    private int barWidth;
    // Managers
    private ActivityManager mActivityManager;
    private WindowManager.LayoutParams mLayoutParams;
    private PackageManager mPackageManager;
    private WindowManager mWindowManager;
    private WidgetbarAppWidgetHost mAppWidgetHost;

    private DisplayMetrics metrics = new DisplayMetrics();
    private static int mCurrentSession = DEFAULT_SESSION;

    private View mRootView;
    private static Widgetbar mInstance = null;

    /**
     * Prepare the views of the widgetbar
     */
    private Widgetbar() {
        Log.d(TAG, "Creating widgetbar");
        mPackageManager = getContext().getPackageManager();
        mActivityManager = ((ActivityManager) getContext().getSystemService(
                "activity"));
        mInflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mAppWidgetManager = AppWidgetManager.getInstance(getContext());
        mAppWidgetHost = new WidgetbarAppWidgetHost(getContext(),
                APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();
        // mAnimator = new WidgetbarAnimator(this);
        Log.d(TAG, "Creating Widgetbar" + getContext());
    }

    private void initViews() {
        mInitialized = false;
        mWindowCreated = false;
        mRootView = mInflater.inflate(R.layout.widgetbar, null);

        mDragLayer = (DragLayer) mRootView.findViewById(R.id.drag_layout);
        final DragLayer dragLayer = mDragLayer;
        mWorkspace = (WidgetbarWorkspace) dragLayer
                .findViewById(R.id.workspace);
        final WidgetbarWorkspace workspace = mWorkspace;
        dragLayer.setDragScoller(workspace);

        mWindow.setContentView(mRootView);
        // mWindow.getWindow().setWindowAnimations(R.style.Animation_Widgetbar);

        WindowManager.LayoutParams lp = mWindow.getWindow().getAttributes();

        if (mWindowManager == null) {
            mWindowManager = (WindowManager) mRootView.getContext()
                    .getApplicationContext().getSystemService("window");
        }
        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenHeight = size.y;
        screenWidth = size.x;
        if (screenHeight > screenWidth) {
            lp.height = (int) (screenHeight * 0.5);
            lp.width = (int) screenWidth;
        } else {
            lp.height = (int) (screenHeight * 0.6);
            lp.width = (int) screenWidth;
        }
        mWindow.getWindow().setAttributes(lp);

    }

    public void destroyWidetbar() {
        if (mWindowAdded) {
            mWindow.getWindow().setWindowAnimations(0);
            mWindow.dismiss();
        }
    }

    public WidgetbarWorkspace getWorkspace() {
        return mWorkspace;
    }

    public WidgetbarWindow getWidetbarWindow() {
        return mWindow;
    }

    public WidgetbarModel getModel() {
        return this.sModel;
    }

    public WidgetbarAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public static Widgetbar getInstance() {
        if (DEBUG)
            Log.d(TAG, "Getting Instance");
        if (mInstance == null) {
            mInstance = new Widgetbar();
        }
        return mInstance;
    }

    public void showWindow() {
        if (mWindow == null) {
            mWindow = new WidgetbarWindow(getContext());
            initViews();
        }

        if (mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }
        mInShowWindow = true;
        mWindowShown = true;
        if (!mItemsLoaded) {
            startLoaders();
        }
        mWindow.show();
        mInShowWindow = false;
        if (DEBUG)
            Log.v(TAG, "showWindow: updating UI");
        if (!mInitialized) {
            mInitialized = true;
        }
    }

    public void hideWindow() {
        Log.d(TAG, "hiding Window");
        if (mWindowShown) {
            mWindow.hide();
            mWindowShown = false;
        }
    }

    public void scrollLeft() {
        mBinder.obtainMessage(WidgetbarBinder.MESSAGE_SCROLL_LEFT)
                .sendToTarget();
    }
    public void scrollRight() {
        mBinder.obtainMessage(WidgetbarBinder.MESSAGE_SCROLL_RIGHT)
                .sendToTarget();
    }

    public void safeShowWindow() {
        mBinder.obtainMessage(WidgetbarBinder.MESSAGE_SHOW_WIDGETBAR)
                .sendToTarget();
    }

    public void safeHideWindow() {
        mBinder.obtainMessage(WidgetbarBinder.MESSAGE_HIDE_WIDGETBAR)
                .sendToTarget();
    }

    public boolean isWindowShown() {
        return mWindowShown;
    }

    /**
     * Bind items to the widgetbar.
     *
     * @param binder
     *            Handler working on the UI Thread
     * @param appWidgets
     *            List of widgets that will be added to the widgetbar. Pop on
     *            each adding and loop until empty.
     */
    private void bindAppWidgets(Widgetbar.WidgetbarBinder binder,
            LinkedList<WidgetbarAppWidgetInfo> appWidgets) {
        final WidgetbarWorkspace workspace = mWorkspace;
        if (!appWidgets.isEmpty()) {
            final WidgetbarAppWidgetInfo item = appWidgets.removeFirst();
            final int appWidgetId = item.getAppWidgetId();
            final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
                    .getAppWidgetInfo(appWidgetId);
            Log.d("Widgetbar", "Binding widget with id:" + appWidgetId);
            item.hostView = mAppWidgetHost.createView(getContext(),
                    appWidgetId, appWidgetInfo);
            item.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            item.hostView.setTag(item);
            workspace.addInSession(item.hostView, item.session, item.cellX,
                    item.cellY, item.spanX, item.spanY);
            workspace.requestLayout();

            binder.obtainMessage(WidgetbarBinder.MESSAGE_BIND_APPWIDGETS)
                    .sendToTarget();
        } else {
            mItemsLoaded = true;
        }
    }

    /**
     * Clear the widgetbar and do binding
     */
    private void bindWidgetbarItems(ArrayList<ItemInfo> items,
            ArrayList<WidgetbarAppWidgetInfo> appWidgets) {
        final WidgetbarWorkspace workspace = mWorkspace;
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            ((ViewGroup) workspace.getChildAt(i)).removeAllViewsInLayout();
        }
        // Flag any old binder to terminate early
        if (mBinder != null) {
            mBinder.mTerminate = true;
        }
        mBinder = new WidgetbarBinder(this, appWidgets);
        mBinder.startBindingAppWidgets();
    }

    public static void setSession(int sessionNum) {
        synchronized (sLock) {
            mCurrentSession = sessionNum;
        }
    }

    public static int getSession() {
        synchronized (sLock) {
            return mCurrentSession;
        }
    }

    public int getBarHeight() {
        return barHeight;
    }

    public int getBarWidth() {
        return barWidth;
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

    /**
     * A handler that bind with the UI thread(The thread creating the widgetbar
     * View) It loads and add Widgets from database to widgetbar
     */
    private static class WidgetbarBinder extends Handler implements
            MessageQueue.IdleHandler {
        static final int MESSAGE_BIND_APPWIDGETS = 0x1;
        static final int MESSAGE_SHOW_WIDGETBAR = 0x2;
        static final int MESSAGE_HIDE_WIDGETBAR = 0x3;
        static final int MESSAGE_SCROLL_LEFT = 0x4;
        static final int MESSAGE_SCROLL_RIGHT = 0x5;
        private final LinkedList<WidgetbarAppWidgetInfo> mAppWidgets;
        private final WeakReference<Widgetbar> mWidgetbar;
        public boolean mTerminate = false;

        WidgetbarBinder(Widgetbar widgetbar,
                ArrayList<WidgetbarAppWidgetInfo> appWidgets) {
            // Only the thread that creating the view can manipulate the view
            super(widgetbar.getWidetbarWindow().getWindow().getContext()
                    .getMainLooper());
            mWidgetbar = new WeakReference<Widgetbar>(widgetbar);

            // sort widgets so active workspace is bound first.
            final int currentSession = widgetbar.mWorkspace.getCurrentSession();
            final int size = appWidgets.size();
            mAppWidgets = new LinkedList<WidgetbarAppWidgetInfo>();

            for (int i = 0; i < size; i++) {
                WidgetbarAppWidgetInfo appWidgetInfo = appWidgets.get(i);
                if (appWidgetInfo.session == currentSession) {
                    mAppWidgets.addFirst(appWidgetInfo);
                } else {
                    mAppWidgets.addLast(appWidgetInfo);
                }
            }
        }

        public boolean queueIdle() {
            // Queue is idle, so start binding items
            startBindingAppWidgets();
            return false;
        }

        public void startBindingAppWidgets() {
            obtainMessage(MESSAGE_BIND_APPWIDGETS).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            Widgetbar widgetbar = mWidgetbar.get();
            if (widgetbar == null || mTerminate) {
                return;
            }
            switch (msg.what) {
            case MESSAGE_BIND_APPWIDGETS:
                widgetbar.bindAppWidgets(this, mAppWidgets);
                break;
            case MESSAGE_SHOW_WIDGETBAR:
                widgetbar.showWindow();
                break;
            case MESSAGE_HIDE_WIDGETBAR:
                widgetbar.hideWindow();
                break;
            case MESSAGE_SCROLL_LEFT:
                mWorkspace.scrollLeft();
                break;
            case MESSAGE_SCROLL_RIGHT:
                mWorkspace.scrollRight();
                break;
            }
        }
    }

}
