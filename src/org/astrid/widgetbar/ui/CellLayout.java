package org.astrid.widgetbar.ui;


import java.util.ArrayList;

import org.astrid.widgetbar.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * The implementation of CellLayout 
 * The CellLayout is the basic layout of the widget bar, it will align the widget to the 
 * Grid
 */
public class CellLayout extends ViewGroup {
	
	//The size of a single cell
	private int mCellWidth;
	private int mCellHeight;
	
	//The padding in the long side
	private int mLongAxisStartPadding;
	private int mLongAxisEndPadding;
	
	//Padding in the short side
	private int mShortAxisStartPadding;
	private int mShortAxisEndPadding;
	
	private int mShortAxisCells;
	private int mLongAxisCells;
	
	private int mWidthGap;
	private int mHeightGap;
	
	private final Rect mRect  = new Rect();
	
	private boolean mDirtyTag;
	
	private final CellInfo mCellInfo  = new CellInfo();
	
	int[] mCellXY = new int[2];
	boolean[][] mOccupied;
	private RectF mDragRect = new RectF();
	
	/**=========================================================
	 * Constructors
	 *================================================================*/
	
	public CellLayout(Context context) {
		this(context, null);
	}
	public CellLayout(Context context, AttributeSet attrs) { 
		this(context, attrs, 0);
	}
	/**
	 * The constructor basically get the parameter from the context (Normally defined in the XML files)
	 * and save the parameters to the local variables
	 */
	public CellLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		//Fetch the attributes for the layout
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
		mCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, 10);
		mCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, 10);
		
		mLongAxisStartPadding = 
				a.getDimensionPixelSize(R.styleable.CellLayout_longAxisStartPadding, 10);
		mLongAxisEndPadding = 
				a.getDimensionPixelSize(R.styleable.CellLayout_longAxisEndPadding, 10);
		mShortAxisStartPadding = 
				a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisStartPadding, 10);
		mShortAxisEndPadding = 
				a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisEndPadding,10);
		mShortAxisCells = a.getInt(R.styleable.CellLayout_shortAxisCells,4);
		mLongAxisCells = a.getInt(R.styleable.CellLayout_longAxisCells,4);
		a.recycle();
		setAlwaysDrawnWithCacheEnabled(false);
		//Create matrix indicating occupied or not
		if(mOccupied == null) {
			mOccupied = new boolean[mLongAxisCells][mShortAxisCells];
		}
	}
		
	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		//Cancel long press for all children
		final int count = getChildCount();
		for(int i=0; i<count; i++) {
			final View child = getChildAt(i);
			child.cancelLongPress();
		}
	}
		
	int getCountX() {
		return mLongAxisCells;
	}
	int getCountY() {
		return mShortAxisCells;
	}
	
	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		//Generate an id for each view, this assumes we have at most 
		//256x256 cells per workspace session
		final LayoutParams cellParams = (LayoutParams) params;
		child.setId(((getId() & 0xFF)<<16)|
				(cellParams.cellX & 0XFF)<<8|(cellParams.cellY & 0XFF));
		super.addView(child, index, params);
	}
	
	@Override
	public void requestChildFocus(View child, View focused) {
		super.requestChildFocus(child, focused);
		//Get the bounding rect of the focused 
		if(child != null) {
			Rect r = new Rect();
			child.getDrawingRect(r);
			requestRectangleOnScreen(r);
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCellInfo.session = ((ViewGroup) getParent()).indexOfChild(this);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final CellInfo cellInfo = mCellInfo;
		
		if(action == MotionEvent.ACTION_DOWN) {
			final Rect frame = mRect;
			//Get the position in respect to the CellLayout
			final int x = (int) ev.getX() + this.getScrollX();
			final int y = (int) ev.getY() + this.getScrollY();
			final int count = getChildCount();
			boolean found = false;
			//Find the View on the cell that touched
			for(int i = count -1; i >= 0; i--) {
				final View child = getChildAt(i);
				if((child.getVisibility()) == VISIBLE ||
						child.getAnimation() != null) {
					child.getHitRect(frame);
					if(frame.contains(x, y)) {
						final LayoutParams lp = (LayoutParams) child.getLayoutParams();
						
						cellInfo.cell = child;
						cellInfo.cellX = lp.cellX;
						cellInfo.cellY = lp.cellY;
						cellInfo.spanX = lp.cellHSpan;
						cellInfo.spanY = lp.cellVSpan;
						cellInfo.valid = true;
						found =true;
						mDirtyTag = false;
						break;
					}
				}
			}
			if(!found) {
				//If no child view here, we record the information of the cell
				//And the cell becomes "dirty"
				int cellXY[] = mCellXY;
				pointToCellExact(x, y, cellXY);
				final int xCount = mLongAxisCells;
				final int yCount = mShortAxisCells;
				
				final boolean[][] occupied = mOccupied;
				findOccupiedCells(xCount, yCount, occupied, null);
				
				cellInfo.cell = null;
				cellInfo.cellX = cellXY[0];
				cellInfo.cellY = cellXY[1];
				cellInfo.spanX = 1;
				cellInfo.spanY = 1;
				cellInfo.valid = cellXY[0] >=0 && cellXY[1] >= 0 && cellXY[0] < xCount &&
						cellXY[1] < yCount && !occupied[cellXY[0]][cellXY[1]];
				//Instead of finding the interesting vacant cells here, wait until a 
				//caller invokes getTag() to retrieve the result. Find the vacant
				//cells is a bit expensive and can generate many new objects, it's 
				//therefore better to defer it until we know we actually need it
				mDirtyTag = true;
			}
			setTag(cellInfo);
		} else if(action == MotionEvent.ACTION_UP) {
			//Clear the state
			cellInfo.cell = null;
			cellInfo.cellX = -1;
			cellInfo.cellY = -1;
			cellInfo.spanX = 0;
			cellInfo.spanY = 0;
			cellInfo.spanX = 0;
			cellInfo.spanY = 0;
			cellInfo.valid = false;
			setTag(cellInfo);
		}
		//Allow the child view to handle the event
		return false;
	}
	/**
	 * For every get tag, it will clear the current vacant cell list, 
	 * and generate a new vacant cell list that is revelent to the current
	 * cell. 
	 */
	@Override
	public CellInfo getTag() {
		final CellInfo info = (CellInfo) super.getTag();
		if(mDirtyTag && info.valid) {
			final int xCount = mLongAxisCells;
			final int yCount = mShortAxisCells;
			
			final boolean[][] occupied = mOccupied;
			findOccupiedCells(xCount, yCount, occupied, null);
			findIntersectingVacantCells(info, info.cellX, info.cellY, xCount, yCount, occupied);
			mDirtyTag = false;
		}
		return info;
	}
	/**
	 * Calculate the nearby vacant that can be expanded from current cell
	 */
	private static void findIntersectingVacantCells(CellInfo cellInfo, int x, int y, 
			int xCount, int yCount, boolean[][] occupied) {
		cellInfo.maxVacantSpanX = Integer.MIN_VALUE;
		//The SpanY of the maxVacantSpanX
		cellInfo.maxVacantSpanXSpanY = Integer.MIN_VALUE;
		cellInfo.maxVacantSpanY = Integer.MIN_VALUE;
		cellInfo.maxVacantSpanYSpanX = Integer.MIN_VALUE;
		cellInfo.clearVacantCells();
		
		if(occupied[x][y]) {
			return;
		}
		
		cellInfo.current.set(x,y,x,y);
		findVacantCell(cellInfo.current, xCount, yCount, occupied, cellInfo);
		
	}
	
	/**
	 * Find vacantCell of different sizes recursively starting from the current cell, add them to the vacant list
	 * once found, basically findIntersectingVacantCell of current
	 */
	private static void findVacantCell(Rect current, int xCount, int yCount, boolean[][] occupied, 
			CellInfo cellInfo) {
		
		addVacantCell(current, cellInfo);
		if(current.left>0) {
			if(isColumnEmpty(current.left -1, current.top, current.bottom, occupied)){ 
				current.left--;
				findVacantCell(current, xCount, yCount, occupied, cellInfo);
				current.left++;
			}
		}
		if(current.right < xCount -1 ) {
			if(isColumnEmpty(current.right + 1, current.top, current.bottom, occupied)) {
				current.right++;
				findVacantCell(current, xCount, yCount, occupied, cellInfo);
				current.right--;
			}
		}
		if(current.top>0){
			if(isRowEmpty(current.top -1, current.left, current.right, occupied)) {
				current.top--;
				findVacantCell(current, xCount, yCount, occupied, cellInfo);
				current.top++;
			}
		}
		if(current.bottom<yCount -1) {
			if(isRowEmpty(current.bottom+1, current.left, current.right, occupied)) {
				current.bottom++;
				findVacantCell(current, xCount, yCount, occupied, cellInfo);
				current.bottom--;
			}
		}
	}
	/**
	 * Insert a vacant cell into the vacant cell list
	 */
	private static void addVacantCell(Rect current, CellInfo cellInfo) {
		CellInfo.VacantCell cell = CellInfo.VacantCell.acquire();
		cell.cellX = current.left;
		cell.cellY = current.top;
		cell.spanX = current.right - current.left + 1;
		cell.spanY = current.bottom - current.top + 1;
		if(cell.spanX > cellInfo.maxVacantSpanX) {
			cellInfo.maxVacantSpanY = cell.spanY;
			cellInfo.maxVacantSpanYSpanX = cell.spanX;
		}
		if(cell.spanY > cellInfo.maxVacantSpanY) {
			cellInfo.maxVacantSpanY = cell.spanY;
			cellInfo.maxVacantSpanYSpanX  = cell.spanX;
		}
		cellInfo.vacantCells.add(cell);
	}
	
	private static boolean isColumnEmpty(int x, int top, int bottom, boolean[][] occupied){
		for(int y = top; y <= bottom; y++) {
			if(occupied[x][y]) {
				return false;
			}
		}
		return true;
	}
	private static boolean isRowEmpty(int y, int left, int right, boolean[][] occupied) {
		for(int x = left; x <- right; x++) {
			if(occupied[x][y]) {
				return false;
			}
		}
		return true;
	}
	
	
		
	CellInfo findAllVacantCells(boolean[] occupiedCells, View ignoreView) {
		final int xCount = mLongAxisCells;
		final int yCount = mShortAxisCells;
		
		boolean[][] occupied = mOccupied;
		if(occupiedCells != null) {
			for(int y = 0; y < yCount; y++){
				for(int x = 0; x < xCount; x++) {
					occupied[x][y] = occupiedCells[y*xCount + x];
				}
			}
		}else{
			findOccupiedCells(xCount, yCount, occupied, ignoreView);
		}
		CellInfo cellInfo  = new CellInfo();
		cellInfo.cellX = -1;
		cellInfo.cellY = -1;
		cellInfo.spanX = 0;
		cellInfo.spanY = 0;
		cellInfo.maxVacantSpanX = Integer.MIN_VALUE;
		cellInfo.maxVacantSpanXSpanY = Integer.MIN_VALUE;
		cellInfo.maxVacantSpanY = Integer.MIN_VALUE;
		cellInfo.maxVacantSpanYSpanX = Integer.MIN_VALUE;
		cellInfo.session = mCellInfo.session;
		Rect current = cellInfo.current;
		
		for(int x = 0; x < xCount; x++) {
			for(int y=0; y < yCount; y++) {
				if(!occupied[x][y]) {
					current.set(x,y,x,y);
					findVacantCell(current, xCount, yCount, occupied, cellInfo);
					occupied[x][y] = true;
				}
			}
		}
		cellInfo.valid = cellInfo.vacantCells.size() > 0;
		//Assume the caller will perform their own cell searching, otherwise we
		//risk causing an unnecessary rebuild after findCellForSpan()
		return cellInfo;
	}
		
	/**
	 * Given a pint, return the cell that strictly encloses that point 
	 * @param x X coord
	 * @param y Y coord
	 * @param result Array of 2 ints to hold the x and y coordinate of the cell
	 */
	void pointToCellExact(int x, int y, int[] result) {
		final int hStartPadding = mLongAxisStartPadding;
		final int vStartPadding = mShortAxisStartPadding;
		result[0] = (x - hStartPadding) / (mCellWidth + mWidthGap);
		result[1] = (y - vStartPadding) / (mCellHeight + mHeightGap);
		
		final int xAxis = mLongAxisCells;
		final int yAxis = mShortAxisCells;
		
		if(result[0] < 0) result[0] = 0;
		if(result[0] >= xAxis) result[0] = xAxis -1;
		if(result[1] < 0) result[1] = 0;
		if(result[1] >= yAxis) result[1] = yAxis -1;
	}
		
	 /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     * 
     * @param cellX X coordinate of the cell 
     * @param cellY Y coordinate of the cell
     * 
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToPoint(int cellX, int cellY, int[] result) {

        final int hStartPadding = mLongAxisStartPadding;
        final int vStartPadding = mShortAxisStartPadding;


        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: currently ignoring padding

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        final int shortAxisCells = mShortAxisCells;
        final int longAxisCells = mLongAxisCells;
        final int longAxisStartPadding = mLongAxisStartPadding;
        final int longAxisEndPadding = mLongAxisEndPadding;
        final int shortAxisStartPadding = mShortAxisStartPadding;
        final int shortAxisEndPadding = mShortAxisEndPadding;
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;


        int numShortGaps = shortAxisCells - 1;
        int numLongGaps = longAxisCells - 1;

        int hSpaceLeft = widthSpecSize - longAxisStartPadding - longAxisEndPadding
               - (cellWidth * longAxisCells);
        mWidthGap = hSpaceLeft / numLongGaps;

        int vSpaceLeft = heightSpecSize - shortAxisStartPadding - shortAxisEndPadding
                - (cellHeight * shortAxisCells);
        if (numShortGaps > 0) {
            mHeightGap = vSpaceLeft / numShortGaps;
        } else {
            mHeightGap = 0;
        }

       int count = getChildCount();

       for (int i = 0; i < count; i++) {
           View child = getChildAt(i);
           LayoutParams lp = (LayoutParams) child.getLayoutParams();

           lp.setup(cellWidth, cellHeight, mWidthGap, mHeightGap, longAxisStartPadding,
                   shortAxisStartPadding);
           int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
           int childheightMeasureSpec =
                   MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
           child.measure(childWidthMeasureSpec, childheightMeasureSpec);
       }

       setMeasuredDimension(widthSpecSize, heightSpecSize);
   }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
            }
        }
    }
	 @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            // Update the drawing caches
            view.buildDrawingCache(true);
        }
    }
	 @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }


	/**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     * 
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param vacantCells Pre-computed set of vacant cells to search.
     * @param recycle Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY,
            CellInfo vacantCells, int[] recycle) {

        // Keep track of best-scoring drop area
        final int[] bestXY = recycle != null ? recycle : new int[2];
        final int[] cellXY = mCellXY;
        double bestDistance = Double.MAX_VALUE;

        // Bail early if vacant cells aren't valid
        if (!vacantCells.valid) {
            return null;
        }

        // Look across all vacant cells for best fit
        final int size = vacantCells.vacantCells.size();
        for (int i = 0; i < size; i++) {
            final CellInfo.VacantCell cell = vacantCells.vacantCells.get(i);

            // Reject if vacant cell isn't our exact size
            if (cell.spanX != spanX || cell.spanY != spanY) {
                continue;
            }

            // Score is center distance from requested pixel
            cellToPoint(cell.cellX, cell.cellY, cellXY);

            double distance = Math.sqrt(Math.pow(cellXY[0] - pixelX, 2) +
                    Math.pow(cellXY[1] - pixelY, 2));
            if (distance <= bestDistance) {
                bestDistance = distance;
                bestXY[0] = cell.cellX;
                bestXY[1] = cell.cellY;
            }
        }

        // Return null if no suitable location found 
        if (bestDistance < Double.MAX_VALUE) {
            return bestXY;
        } else {
            return null;
        }
    }
    /**
     * Drop a child at the specified position
     *
     * @param child The child that is being dropped
     * @param targetXY Destination area to move to
     */
    void onDropChild(View child, int[] targetXY) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.cellX = targetXY[0];
        lp.cellY = targetXY[1];
        lp.isDragging = false;
        mDragRect.setEmpty();
        child.requestLayout();
        invalidate();
    }
    void onDropAborted(View child) {
        if (child != null) {
            ((LayoutParams) child.getLayoutParams()).isDragging = false;
            invalidate();
        }
        mDragRect.setEmpty();
    }
    /**
     * Start dragging the specified child
     * 
     * @param child The child that is being dragged
     */
    void onDragChild(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.isDragging = true;
        mDragRect.setEmpty();
    }
   
    /**
     * Drag a child over the specified position
     * 
     * @param child The child that is being dropped
     * @param cellX The child's new x cell location
     * @param cellY The child's new y cell location 
     */
    void onDragOverChild(View child, int cellX, int cellY) {
        int[] cellXY = mCellXY;
        pointToCellRounded(cellX, cellY, cellXY);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        cellToRect(cellXY[0], cellXY[1], lp.cellHSpan, lp.cellVSpan, mDragRect);
        invalidate();
    }

    /**
     * Computes a bounding rectangle for a range of cells
     *  
     * @param cellX X coordinate of upper left corner expressed as a cell position
     * @param cellY Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan Width in cells 
     * @param cellVSpan Height in cells
     * @param dragRect Rectnagle into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, RectF dragRect) {
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        final int widthGap = mWidthGap;
        final int heightGap = mHeightGap;

        final int hStartPadding = mLongAxisStartPadding;
        final int vStartPadding =  mShortAxisStartPadding;

        int width = cellHSpan * cellWidth + ((cellHSpan - 1) * widthGap);
        int height = cellVSpan * cellHeight + ((cellVSpan - 1) * heightGap);

        int x = hStartPadding + cellX * (cellWidth + widthGap);
        int y = vStartPadding + cellY * (cellHeight + heightGap);

        dragRect.set(x, y, x + width, y + height);
    }
    /**
     * Computes the required horizontal and vertical cell spans to always 
     * fit the given rectangle.
     *  
     * @param width Width in pixels
     * @param height Height in pixels
     */
    public int[] rectToCell(int width, int height) {
        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
        int actualWidth = mCellWidth + mWidthGap;
        int actualHeight = mCellHeight + mHeightGap;
        int smallerSize = Math.min(actualWidth, actualHeight);

        // Always round up to next largest cell
        int spanX = (width + smallerSize) / smallerSize;
        int spanY = (height + smallerSize) / smallerSize;
        return new int[] { spanX, spanY };
    }

    /**
     * Find the first vacant cell, if there is one.
     *
     * @param vacant Holds the x and y coordinate of the vacant cell
     * @param spanX Horizontal cell span.
     * @param spanY Vertical cell span.
     * 
     * @return True if a vacant cell was found
     */
    public boolean getVacantCell(int[] vacant, int spanX, int spanY) {
        final int xCount = mLongAxisCells;
        final int yCount = mShortAxisCells;
        final boolean[][] occupied = mOccupied;

        findOccupiedCells(xCount, yCount, occupied, null);

        return findVacantCell(vacant, spanX, spanY, xCount, yCount, occupied);
    }

    static boolean findVacantCell(int[] vacant, int spanX, int spanY,
            int xCount, int yCount, boolean[][] occupied) {
    	for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                boolean available = !occupied[x][y];
out:            for (int i = x; i < x + spanX - 1 && x < xCount; i++) {
                    for (int j = y; j < y + spanY - 1 && y < yCount; j++) {
                        available = available && !occupied[i][j];
                        if (!available) break out;
                    }
                }

                if (available) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }

        return false;
    }
	boolean[] getOccupiedCells() {
	    final int xCount =  mLongAxisCells;
	    final int yCount =  mShortAxisCells;
	    final boolean[][] occupied = mOccupied;
	
	    findOccupiedCells(xCount, yCount, occupied, null);
	
	    final boolean[] flat = new boolean[xCount * yCount];
	    for (int y = 0; y < yCount; y++) {
	        for (int x = 0; x < xCount; x++) {
	            flat[y * xCount + x] = occupied[x][y];
	        }
	    }
	
	    return flat;
	}
	private void findOccupiedCells(int xCount, int yCount, boolean[][] occupied, View ignoreView) {
				
	    for (int x = 0; x < xCount; x++) {
	        for (int y = 0; y < yCount; y++) {
	            occupied[x][y] = false;
	        }
	    }
	
	    int count = getChildCount();
	    for (int i = 0; i < count; i++) {
	        View child = getChildAt(i);
	        /*
	        if (child instanceof Folder || child.equals(ignoreView)) {
	            continue;
	        }
	        */
	        LayoutParams lp = (LayoutParams) child.getLayoutParams();
	
	        for (int x = lp.cellX; x < lp.cellX + lp.cellHSpan && x < xCount; x++) {
	            for (int y = lp.cellY; y < lp.cellY + lp.cellVSpan && y < yCount; y++) {
	                occupied[x][y] = true;
	            }
	        }
	    }
	}
	@Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }




	public static class LayoutParams extends ViewGroup.MarginLayoutParams {
		/**
		 * Horizontal location of the item in the grid.
		 */
		public int cellX;
		/**
		 * Vertical location of the item in the grid
		 */
		public int cellY;
		/**
		 * Number of cells spanned horizontally by the item
		 */
		public int cellHSpan;
		/**
		 * Number of cells spanned vertically by the item
		 */
		public int cellVSpan;
		/**
		 * Is this item currently being dragged
		 */
		public boolean isDragging;
		
		// X coordinate of the view in the layout
		int x;
		// Y coordinate of the view in the layout 
		int y;
		
		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
			cellHSpan = 1;
			cellVSpan = 1;
		}
		
		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
			cellHSpan = 1;
			cellVSpan = 1;
		}
		
		public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
			super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			this.cellX = cellX;
			this.cellY = cellY;
			this.cellHSpan = cellHSpan;
			this.cellVSpan = cellVSpan;
		}
		public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap, 
				int hStartPadding, int vStartPadding) {
			final int myCellHSpan = cellHSpan;
			final int myCellVSpan = cellVSpan;
			final int myCellX = cellX;
			final int myCellY = cellY;
			width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
					leftMargin - rightMargin;
			height = myCellVSpan * cellHeight + ((myCellVSpan -1) * heightGap) - 
					topMargin - bottomMargin;
			x = hStartPadding + myCellX * (cellWidth + widthGap) + leftMargin;
			y = vStartPadding + myCellY * (cellHeight + heightGap) + topMargin;
		}
	}
	/**
	 * A class that store the information of cells
	 */
	public static final class CellInfo implements ContextMenu.ContextMenuInfo {
		/**
		 * Recycle the vacant cells instances because up to several hundlers can
		 * be instanciated when the user long press an empty cell.
		 * 
		 * VacantCell is basically a linked list storing a pool of vacant cell, for 
		 * quick instantiation.
		 */
		
		static final class VacantCell {
			//Information of the current VacantCell
			int cellX;
			int cellY;
			int spanX;
			int spanY;
			//We can create up to 523 vacant cells on a 4X4 grid
			//100 seems  a reasonable compromise given the size of VacantCell and
			//the user is not likely to touch empty 4X4 grid very often
			//The class maintains a cyclic linked-list for vacantCell pool
			private static int POOL_LIMIT = 100;
			//Lock of the list
			private static final Object sLock = new Object();
			
			private static int sAcquiredCount = 0;
			//The root of the list
			private static VacantCell sRoot;
			
			private VacantCell next;
			/**
			 * Function to acquire a Vacant Cell
			 * Instead of creating a new instance on every acquisition, we look up 
			 * the pool. 
			 */
			static VacantCell acquire() {
				synchronized(sLock) {
					if(sRoot == null) {
						return new VacantCell();
					}
					VacantCell info = sRoot;
					sRoot = info.next;
					sAcquiredCount--;
					return info;
				}
			}
			
			//Release the vacant cell, instead of release to the garbage 
			//Collection, we add it to the pool for the future reuse. 
			void release() {
				synchronized(sLock) {
					if(sAcquiredCount < POOL_LIMIT) {
						sAcquiredCount++;
						next = sRoot;
						sRoot = this;
					}
				}
			}
			
			//Print the information of the current vacant cells
			@Override 
			public String toString() {
				return "VacantCell[x=" + cellX + ", y=" + cellY + ", spanX="+spanX +
						", spanY=" + spanY + "]";
			}
		}
		
		View cell;
		int cellX;
		int cellY;
		int spanX;
		int spanY;
		//The session that the cell belong to
		public int session;
		boolean valid;
		
		//Maintain a list of vacant cells
		final ArrayList<VacantCell> vacantCells = new ArrayList<VacantCell>(VacantCell.POOL_LIMIT);
		
		int maxVacantSpanX;
		int maxVacantSpanXSpanY;
		int maxVacantSpanY;
		int maxVacantSpanYSpanX;
		final Rect current = new Rect();
		
		void clearVacantCells() {
			final ArrayList<VacantCell> list = vacantCells;
			final int count = list.size();
			
			for(int i = 0; i < count; i++) list.get(i).release();
			
			list.clear();
		}
		
		void findVacantCellsFromOccupied(boolean[] occupied, int xCount, int yCount) {
			if(cellX < 0 || cellY < 0) {
				maxVacantSpanX = maxVacantSpanXSpanY = Integer.MIN_VALUE;
				maxVacantSpanY = maxVacantSpanYSpanX = Integer.MIN_VALUE; 
				clearVacantCells();
				return;
			}
			
			final boolean[][] unflattened = new boolean[xCount][yCount];
			for(int y = 0; y < yCount; y++) {
				for(int x = 0; x < xCount; x++) {
					unflattened[x][y] = occupied[y*xCount + x];
				}
			}
			CellLayout.findIntersectingVacantCells(this, cellX, cellY, xCount, yCount, unflattened);
		}
		/**
		 * This method can be called only once! Calling findVacantCellsFromOccupied will
		 * restore the ability to call this method.
		 * 
		 * Finds the upper-left coordinate of the first rectangle in the grid that can hold
		 * a cell of the specified dimensions.
		 * 
		 * @param cellXY The array that will contain the position of the vacant cell if such a cell 
		 *        can be found.
		 * @Param spanX The horizontal span of the cell we want to find
		 * @Param spanY The vertical span of the cell we want to find.
		 * 
		 * @return True if a vacant cell of the specified dimension was found, false otherwise.
		 */
		public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
			return findCellForSpan(cellXY, spanX, spanY, true);
		}
		
		
		/**
		 * The real implementation if findCellForSpan, iterate through the list to 
		 * find a cell that big enough for span
		 */
		boolean findCellForSpan(int[] cellXY, int spanX, int spanY, boolean clear) {
			final ArrayList<VacantCell> list = vacantCells;
			final int count = list.size();
			boolean found =false;
			if(this.spanX >= spanX && this.spanY >= spanY) {
				cellXY[0] = cellX;
				cellXY[1] = cellY;
				found = true;
			}
			//Look for an exact match first
			for(int i=0; i < count; i++) {
				VacantCell cell = list.get(i);
				if(cell.spanX == spanX && cell.spanY == spanY) {
					cellXY[0] = cell.cellX;
					cellXY[1] = cell.cellY;
					found = true;
					break;
				}
			}
			//look for the first cell large enough
			for(int i = 0; i < count; i++) {
				VacantCell cell = list.get(i);
				if(cell.spanX >= spanX && cell.spanY >= spanY) {
					cellXY[0] = cell.cellX;
					cellXY[1] = cell.cellY;
					found = true;
					break;
				}
			}
			if(clear) clearVacantCells();
			return found;
		}
		@Override
		public String toString() {
			return "Cell[View=" + (cell == null ? "null": cell.getClass()) + ", x=" + cellX +
					", y=" + cellY + "]";
		}
		
	}
	
}

