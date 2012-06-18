package org.astrid.widgetbar.ui;

import android.view.VelocityTracker;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * The workspace is a wide area with a finite number of sessions. Each
 * session contains a number of Widgets the user can interact with.
 * A Workspace is meant to be used with a fixed width only
 */

public class WidgetBarWorkspace extends ViewGroup {
	private static final int INVALID_SESSION = -1;
	/**
	 * The velocity at which a fling gesture will cause us to snap to next screen
	 */
	private static final int SNAP_VELOCITY = 1000;
	
	private int mDefaultSession;
	
	private boolean mFirstLayout = true;
	
	private int mCurrentSession;
	private int mNextSession = INVALID_SESSION;
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	
	

}
