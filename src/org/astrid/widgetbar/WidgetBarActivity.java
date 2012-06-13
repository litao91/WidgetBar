package org.astrid.widgetbar;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class WidgetBarActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	this.startService(new Intent(this, widgetBarService.class));
    }
}