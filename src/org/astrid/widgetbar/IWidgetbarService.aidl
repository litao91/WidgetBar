//IWigetbarService.aidl
package org.astrid.widgetbar;

interface IWidgetbarService {
	void showWidgetbar();
	void hideWidgetbar();
    boolean onActivityKeyPressed(in int keyCode);
}
