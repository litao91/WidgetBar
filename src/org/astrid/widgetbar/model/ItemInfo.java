package org.astrid.widgetbar.model;



import android.content.ContentValues;


public class ItemInfo {
	public static final int NO_ID = -1;
	/**
	 * The id in the setting data base for this item
	 */
	public long id = NO_ID;
	
	/**
	 * Currently widget Only, but probably shortcut, folder, or appwidget later
	 */ 
	public int itemType;
	
	/**
	 * Indicates the workspace in which the shortcut appears
	 */
	public int session = -1;
	
	/**
	 * Indicates the X position of the associated cell
	 */
	public int cellX = -1;
	
	/**
	 * Indicates the Y position of teh associated cell.
	 */
	public int cellY = -1;
	
	/**
	 * Indicates the X cell span.
	 */
	public int spanX =1;
	
	/**
	 * Indicate the Y cell span.
	 */
	public int spanY = 1;
	
	ItemInfo(){
	}
	
	ItemInfo(ItemInfo info) {
		id = info.id;
		cellX = info.cellX;
		cellY = info.cellY;
		spanX = info.spanX;
		spanY = info.spanY;
		itemType = info.itemType;
	}
	/**
	 * Write the fields of this item to the DB
	 * 
	 * @param values
	 */   
	void onAddToDatabase(ContentValues values) {
		values.put(WidgetbarSettings.BaseBarColumns.ITEM_TYPE, itemType);
		values.put(WidgetbarSettings.Favoriates.SPACE_SESSION, session);
		values.put(WidgetbarSettings.Favoriates.CELLX, cellX);
		values.put(WidgetbarSettings.Favoriates.CELLY, cellY);
		values.put(WidgetbarSettings.Favoriates.SPANX, spanX);
		values.put(WidgetbarSettings.Favoriates.SPANY, spanY);
	}
}
