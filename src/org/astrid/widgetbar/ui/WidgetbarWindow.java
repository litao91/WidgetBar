package org.astrid.widgetbar.ui;

import org.astrid.widgetbar.R;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

public class WidgetbarWindow extends Dialog {
	public final static String TAG = "WidgetbarWindow";
	//private final Rect mBounds = new Rect();

	public WidgetbarWindow(Context context) {
		super(context,R.style.FullHeightDialog);
		Log.d(TAG,"Creating widgetbar window in"+context);
		initDockWindow();
	}
	/**
	 * get the size of the DockWindow
	 * 
	 * @return if the DockWindow sticks to the top or bottom of the screen, the
	 *         return value is the height of the DockWindow, and its width is
	 *         equal to the width of the screen; if the DockWindow sticks to the
	 *         left or right of the screen, the return value is the width of the
	 *         DockWindow, and its height is equal to the height of the screen
	 */
	public int getSize() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();

		if (lp.gravity == Gravity.TOP || lp.gravity == Gravity.BOTTOM) {
			return lp.height;
		} else {
			return lp.width;
		}
	}

	/**
	 * Set the size of the DockWindow
	 * 
	 * @param size
	 *            If the DockWindow sticks to the top or bottom of the screen,
	 *            size is the height, and its width is equal to the width of the
	 *            screen; if the DockWindow sticks to the left or right of the
	 *            screen, size is the width of the DockWindow, and its height is
	 *            equal to the height of the screen
	 */
	public void setSize(int size) {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		if (lp.gravity == Gravity.TOP || lp.gravity == Gravity.BOTTOM) {
			lp.width = -1;
			lp.height = size;
		} else {
			lp.width = size;
			lp.height = -1;
		}
		getWindow().setAttributes(lp);
	}

	/**
	 * Set which boundary of the screen the DockWindow sticks to
	 * 
	 * @param gravity
	 *            the boundary of the screen to stick
	 */
	public void setGravity(int gravity) {
		WindowManager.LayoutParams lp = getWindow().getAttributes();

		boolean oldIsVertical = (lp.gravity == Gravity.TOP || lp.gravity == Gravity.BOTTOM);

		lp.gravity = gravity;

		boolean newIsVertical = (lp.gravity == Gravity.TOP || lp.gravity == Gravity.BOTTOM);

		if (oldIsVertical != newIsVertical) {
			int tmp = lp.width;
			lp.width = lp.height;
			lp.height = tmp;
			getWindow().setAttributes(lp);
		}

	}
	
	private void initDockWindow() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		
		//The layout is basically similar to a input method.
		//lp.type = WindowManager.LayoutParams.TYPE_INPUT_METHOD;
		lp.setTitle("Widgetbar");
		
		lp.gravity = Gravity.BOTTOM;
		lp.width = -1;
		//Let the window's orientation follow sensor based rotation
		// Turn off for now
		//lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
		getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		
		getWindow().setAttributes(lp);
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
				WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	}
	
}
