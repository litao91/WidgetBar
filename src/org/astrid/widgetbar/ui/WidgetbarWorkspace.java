package org.astrid.widgetbar.ui;

import org.astrid.widgetbar.R;
import org.astrid.widgetbar.model.Widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

//import android.util.Log;

/**
 * The workspace is a wide area with a finite number of sessions. Each session
 * contains a number of Widgets the user can interact with. A Workspace is meant
 * to be used with a fixed width only
 */

public class WidgetbarWorkspace extends ViewGroup implements DragScroller {
	private static final int INVALID_SESSION = -1;
	/**
	 * The velocity at which a fling gesture will cause us to snap to next
	 * screen
	 */
	private static final int SNAP_VELOCITY = 1000;

	private final static int TOUCH_STATE_REST = 0;
	private final static int TOUCH_STATE_SCROLLING = 1;

	private int mDefaultSession;

	private boolean mFirstLayout = true;

	private int mCurrentSession;
	private int mNextSession = INVALID_SESSION;
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mMaximumVelocity;
	private int mTouchState = TOUCH_STATE_REST;
	private int mTouchSlop;

	private int[] mTempCell = new int[2];

	private float mLastMotionX;
	private float mLastMotionY;

	/**
	 * Used to inflate the Workspace from XML
	 *
	 * @param context
	 *            The applicatin's context
	 * @param attrs
	 *            The attributes set containing the Workspace's customization
	 *            values
	 */
	public WidgetbarWorkspace(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Used to Inflate the Workspace from XML
	 *
	 * @param context
	 *            The application's context
	 * @param attrs
	 *            The attributes set containing the Workspace's customization
	 *            values
	 * @param defStyle
	 *            Unused.
	 */
	public WidgetbarWorkspace(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// Get the attributes
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.Workspace, defStyle, 0);
		mDefaultSession = a.getInt(R.styleable.Workspace_defaultSession, 1);
		a.recycle();
		initWorkspace();
	}

	private void initWorkspace() {
		mScroller = new Scroller(getContext());
		mCurrentSession = mDefaultSession;
		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		mTouchSlop = configuration.getScaledTouchSlop();
	}

	@Override
	public void addView(View child, int index, LayoutParams params) {
		if (!(child instanceof CellLayout)) {
			throw new IllegalArgumentException(
					"A Workspace can only have CellLayout children");
		}
		super.addView(child, index, params);
	}

	@Override
	public void addView(View child) {
		if (!(child instanceof CellLayout)) {
			throw new IllegalArgumentException(
					"A Workspace can only have CellLayout children");
		}
		super.addView(child);
	}

	@Override
	public void addView(View child, int index) {
		if (!(child instanceof CellLayout)) {
			throw new IllegalArgumentException(
					"A Workspace can only have CellLayout children");
		}
		super.addView(child, index);
	}

	@Override
	public void addView(View child, int width, int height) {
		if (!(child instanceof CellLayout)) {
			throw new IllegalArgumentException(
					"A Workspace can only have CellLayout children");
		}
		super.addView(child, width, height);
	}

	@Override
	public void addView(View child, LayoutParams params) {
		if (!(child instanceof CellLayout)) {
			throw new IllegalArgumentException(
					"A Workspace can only have CellLayout children");
		}
		super.addView(child, params);
	}

	boolean isDefaultSessionShowing() {
		return mCurrentSession == mDefaultSession;
	}

	/**
	 * @return The index of the currently displayed session
	 */
	public int getCurrentSession() {
		return mCurrentSession;
	}

	/**
	 * Computes a bounding rectangle for a range of cells
	 *
	 * @param CellX
	 *            X coord. of upper left corner expressed as a cell position
	 * @param CellY
	 *            Y coord. of upper left corner expressed as a cell position
	 * @param cellHSpan
	 *            Width in cells
	 * @param cellVSpan
	 *            Height in cells
	 * @param rect
	 *            Rectangle into which to put the result
	 */
	public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan,
			RectF rect) {
		((CellLayout) getChildAt(mCurrentSession)).cellToRect(cellX, cellY,
				cellHSpan, cellVSpan, rect);
	}

	/**
	 * Sets the current session
	 *
	 * @param currentSession
	 */
	void setCurrentSession(int currentSession) {
		mCurrentSession = Math.max(0,
				Math.min(currentSession, getChildCount() - 1));
		scrollTo(mCurrentSession * getWidth(), 0);
		invalidate();
	}

	void showDefaultSession() {
		setCurrentSession(mDefaultSession);
	}

	/**
	 * Adds the specified child in the current screen.
	 *
	 * @param child
	 *            The child to add in one of the workspace's sessions.
	 * @param x
	 *            The X position of the child in the screen's grid
	 * @param y
	 *            The Y position of the child in the screen's grid
	 * @param spanX
	 *            The number of cells spanned horizontally by the child
	 * @param spanY
	 *            The number of cells spanned vertically by the child
	 */
	public void addInCurrentSession(View child, int x, int y, int spanX,
			int spanY, boolean insert) {
		addInSession(child, mCurrentSession, x, y, spanX, spanY, insert);
	}

	/**
	 * Adds the specified child in the specified screen.
	 *
	 * @param child
	 *            The child to add in the session
	 * @param session
	 *            The session in which to add the child.
	 * @param x
	 *            The X position of the child in teh screen's grid
	 * @param y
	 *            The Y position of the child in the screen's grid
	 * @param spanX
	 *            The number of cells spanned horizontally by the child.
	 * @param spanY
	 *            The number of cells spanned vertically by the child
	 */
	void addInSession(View child, int session, int x, int y, int spanX,
			int spanY) {
		addInSession(child, session, x, y, spanX, spanY, false);
	}

	void addInSession(View child, int x, int y, int spanX, int spanY,
			boolean insert) {
		addInSession(child, mCurrentSession, x, y, spanX, spanY, insert);
	}

	/**
	 * Adds the specified child in the specified session
	 *
	 * @param insert
	 *            When true, the child is inserted at the beginning of the
	 *            children list.
	 */
	void addInSession(View child, int session, int x, int y, int spanX,
			int spanY, boolean insert) {
		if (session < 0 || session >= getChildCount()) {
			throw new IllegalStateException("The screen must be >= 0 and < "
					+ getChildCount());
		}

		final CellLayout group = (CellLayout) getChildAt(session);
		CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child
				.getLayoutParams();

		if (lp == null) {
			lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
		} else {
			lp.cellX = x;
			lp.cellY = y;
			lp.cellHSpan = spanX;
			lp.cellVSpan = spanY;
		}
		group.addView(child, insert ? 0 : -1, lp);
	}

	void addWidget(View view, Widget widget) {
		addInSession(view, widget.session, widget.cellX, widget.cellY,
				widget.spanX, widget.spanY, false);
	}

	void addWidget(View view, Widget widget, boolean insert) {
		addInSession(view, widget.session, widget.cellX, widget.cellY,
				widget.spanX, widget.spanY, insert);
	}

	public CellLayout.CellInfo findAllVacantCells(boolean[] occupied) {
		CellLayout group = (CellLayout) getChildAt(mCurrentSession);
		if (group != null) {
			return group.findAllVacantCells(occupied, null);
		}
		return null;
	}

	/**
	 * @return The coordinate of a vacant cell for the current session
	 */
	boolean getVacantCell(int[] vacant, int spanX, int spanY) {
		CellLayout group = (CellLayout) getChildAt(mCurrentSession);
		if (group != null) {
			return group.getVacantCell(vacant, spanX, spanY);
		}
		return false;
	}

	/**
	 * Adds the specified childi in the current session
	 *
	 * @param child
	 *            The child to add in one of the workspace's sessions.
	 * @param spanX
	 *            The number of the cells spanned horizontally by the child
	 * @param spanY
	 *            the number of cells spanned vertically by the child.
	 */
	void fitInCurrentSession(View child, int spanX, int spanY) {
		fitInSession(child, mCurrentSession, spanX, spanY);
	}

	void fitInSession(View child, int session, int spanX, int spanY) {
		if (session < 0 || session > getChildCount()) {
			throw new IllegalStateException("The screen must be >= 0 and < "
					+ getChildCount());
		}
		final CellLayout group = (CellLayout) getChildAt(session);
		boolean vacant = group.getVacantCell(mTempCell, spanX, spanY);
		if (vacant) {
			group.addView(child, new CellLayout.LayoutParams(mTempCell[0],
					mTempCell[1], spanX, spanY));
		}
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			setScrollX(mScroller.getCurrX());
			setScrollY(mScroller.getCurrY());
			postInvalidate();
		} else if (mNextSession != INVALID_SESSION) {
			mCurrentSession = Math.max(0,
					Math.min(mNextSession, getChildCount() - 1));
			Widgetbar.setSession(mCurrentSession);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		// int height = mBarView.getBarHeight();
		// int width = mBarView.getBarWidth();
		// canvas.drawARGB(50, 233, 233, 233);
		super.dispatchDraw(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"Workspace can only be used in EXACTLY mode.");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"Workspace can only be used in EXACTLY mode.");
		}

		// The children are given the same width and height as the workspace
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}

		if (mFirstLayout) {
			scrollTo(mCurrentSession * width, 0);
			mFirstLayout = false;
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		int childLeft = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				final int childWidth = child.getMeasuredWidth();
				child.layout(childLeft, 0, childLeft + childWidth,
						child.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	}

	/*
	 * @Override protected boolean requestChildRectangleOnScreen(View child,
	 * Rect rectangle, boolean immediate) { int session = indexOfChild(child);
	 * if(session != mCurrentSession || !mScroller.isFinished()) {
	 * snapToSession(session); return true; } return false; }
	 */

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		/**
		 * This determine whether we want to intercept the motion. If we return
		 * true, onTouchEvent will be called and we do the actual scrolling
		 * there If we return false, the event will be dispatched to the child
		 * view, which will handle the event
		 */
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			// Log.d("Workspace", "Moving and Scrolling");
			return true;
		}
		final float x = ev.getX();
		final float y = ev.getY();
		switch (action) {
		case MotionEvent.ACTION_MOVE:
			// Log.d("Workspace", "Intercepting Move");
			final int xDiff = (int) Math.abs(x - mLastMotionX);
			final int yDiff = (int) Math.abs(y - mLastMotionY);

			final int touchSlop = mTouchSlop;
			boolean xMoved = xDiff > touchSlop;
			boolean yMoved = yDiff > touchSlop;

			if (xMoved || yMoved) {
				if (xMoved) {
					// Scroll if the user moved far enough along the X axis
					// Log.d("Workspace", "Moved");
					mTouchState = TOUCH_STATE_SCROLLING;
					enableChildrenCache();
				}
			}
			break;
		case MotionEvent.ACTION_DOWN:
			// Log.d("Workspace", "Intercepting Down");
			// Remember the location of down touch
			mLastMotionX = x;
			mLastMotionY = y;
			/*
			 * If being flinged and user touches the screen, initiate drag;
			 * otherwise don't. mScroller.isFinished should be false when being
			 * flinged.
			 */
			// Log.d("Workspace",
			// "Scroller is finished:"+mScroller.isFinished());
			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;

			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// Release the drag
			clearChildrenCache();
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		/**
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode
		 */
		return mTouchState != TOUCH_STATE_REST;
		// return false;
	}

	void enableChildrenCache() {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final CellLayout layout = (CellLayout) getChildAt(i);
			layout.setChildrenDrawnWithCacheEnabled(true);
			layout.setChildrenDrawingCacheEnabled(true);
		}
	}

	void clearChildrenCache() {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final CellLayout layout = (CellLayout) getChildAt(i);
			layout.setChildrenDrawnWithCacheEnabled(false);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
		final int action = ev.getAction();
		final float x = ev.getX();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			/*
			 * If being fliinged and user touches, stop teh fling. isFinished
			 * will be false if being flinged.
			 */
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			// Remember where the motion event started
			mLastMotionX = x;
			// When touch a empty space(The down event wasn't handled by the
			// Child), do scrolling
			mTouchState = TOUCH_STATE_SCROLLING;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				// Scroll to follow the motion event
				final int deltaX = (int) (mLastMotionX - x);
				mLastMotionX = x;
				if (deltaX < 0) {
					if (getScrollX() > 0) {
						scrollBy(Math.max(-getScrollX(), deltaX), 0);
					}
				} else if (deltaX > 0) {
					final int avaliableToScroll = getChildAt(
							getChildCount() - 1).getRight()
							- getScrollX() - getWidth();
					if (avaliableToScroll > 0) {
						scrollBy(Math.min(avaliableToScroll, deltaX), 0);
					}
				}

			}
			break;
		case MotionEvent.ACTION_UP:
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				int velocityX = (int) velocityTracker.getXVelocity();

				if (velocityX > SNAP_VELOCITY && mCurrentSession > 0) {
					// Fling hard enough to move left
					snapToSession(mCurrentSession - 1);
				} else if (velocityX < -SNAP_VELOCITY && mCurrentSession > 0) {
					// Fling hard enough to move right
					snapToSession(mCurrentSession + 1);
				} else {
					snapToDestination();
				}
				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
			}
			mTouchState = TOUCH_STATE_REST;
			break;
		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
		}
		return true;
	}

	private void snapToDestination() {
		final int sessionWidth = getWidth();
		final int whichSession = (getScrollX() + (sessionWidth / 2))
				/ sessionWidth;
		snapToSession(whichSession);
	}

	void snapToSession(int whichSession) {
		if (!mScroller.isFinished())
			return;
		enableChildrenCache();
		whichSession = Math.max(0, Math.min(whichSession, getChildCount() - 1));
		boolean changingSessions = whichSession != mCurrentSession;

		mNextSession = whichSession;
		View focusedChild = getFocusedChild();
		if (focusedChild != null && changingSessions
				&& focusedChild == getChildAt(mCurrentSession)) {
			focusedChild.clearFocus();
		}
		final int newX = whichSession * getWidth();
		final int delta = newX - getScrollX();
		mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta) * 2);
		invalidate();
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		if (savedState.currentSession != -1) {
			mCurrentSession = savedState.currentSession;
			Widgetbar.setSession(mCurrentSession);
		}
	}

	public void scrollLeft() {
		if (mScroller.isFinished()) {
			snapToSession((mCurrentSession - 1) % getChildCount());
		}
	}

	public void scrollRight() {
		if (mScroller.isFinished()) {
			snapToSession((mCurrentSession + 1) % getChildCount());
		}
	}

	public static class SavedState extends BaseSavedState {
		int currentSession = -1;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			currentSession = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(currentSession);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}

			public SavedState createFromParcel(Parcel in) {
				// TODO Auto-generated method stub
				return new SavedState(in);
			}
		};
	}

}
