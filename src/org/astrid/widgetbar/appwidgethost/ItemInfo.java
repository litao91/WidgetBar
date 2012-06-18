package org.astrid.widgetbar.appwidgethost;

import android.content.ContentValues;


public class ItemInfo {
	static final int NO_ID = -1;
	/**
	 * The id in the setting data base for this item
	 */
	long id = NO_ID;
	
	/**
	 * Currently widget Only, but probably shortcut, folder, or appwidget later
	 */ 
	int itemType;
	
	/**
	 * Indicates the workspace in which the shortcut appears
	 */
	int spaceSession = -1;
	
	/**
	 * Indicates the X position of the associated cell
	 */
	int cellX = -1;
	
	/**
	 * Indicates the Y position of teh associated cell.
	 */
	int cellY = -1;
	
	/**
	 * Indicates the X cell span.
	 */
	int spanX =1;
	
	/**
	 * Indicate the Y cell span.
	 */
	int spanY = 1;
	
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
		values.put(WidgetBarSettings.BaseBarColumns.ITEM_TYPE, itemType);
		values.put(WidgetBarSettings.Favoriates.SPACE_SESSION, spaceSession);
		values.put(WidgetBarSettings.Favoriates.CELLX, cellX);
		values.put(WidgetBarSettings.Favoriates.CELLY, cellY);
		values.put(WidgetBarSettings.Favoriates.SPANX, spanX);
		values.put(WidgetBarSettings.Favoriates.SPANY, spanY);
	}
}
