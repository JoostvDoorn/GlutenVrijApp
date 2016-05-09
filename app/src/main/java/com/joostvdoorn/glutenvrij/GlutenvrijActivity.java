package com.joostvdoorn.glutenvrij;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.joostvdoorn.glutenvrij.scanner.CaptureActivity;
import com.joostvdoorn.glutenvrij.scanner.PreferencesActivity;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.Activity;
import android.widget.TextView.OnEditorActionListener;

public class GlutenvrijActivity extends TrackedActivity implements View.OnClickListener, OnEditorActionListener {
	protected static final String NAME = "Homescreen";
	private static final int SETTINGS_ID = Menu.FIRST;
	private static final int ABOUT_ID = Menu.FIRST + 1;
	public static final String VERSION_NAME = "1.04";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, this.NAME);

        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
        
        setContentView(R.layout.main);
        
        Button searchButton = (Button) findViewById(R.id.searchButton);
        EditText searchText = (EditText) findViewById(R.id.searchText);
        searchButton.setOnClickListener(this);
        searchText.setOnEditorActionListener(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);


    }

    public void scanBarcodes(View view) {
    	Intent myIntent = new Intent(view.getContext(), CaptureActivity.class);
    	startActivity(myIntent);
    }

    private void search(View view) {
    	Intent myIntent = new Intent(view.getContext(), SearchActivity.class);
    	myIntent.putExtra("com.joostvdoorn.glutenvrij.SearchValue", ((EditText) findViewById(R.id.searchText)).getText().toString());
    	myIntent.putExtra("com.joostvdoorn.glutenvrij.SearchCategory", Integer.toString(((Spinner) findViewById(R.id.spinner)).getBaseline()));
    	startActivity(myIntent);
    }
	@Override
	public void onClick(View view) {
    	search(view);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if(actionId == EditorInfo.IME_NULL){
			search(v);
			return true;
		}
		return false;
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, SETTINGS_ID, 0, R.string.menu_settings)
		.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, ABOUT_ID, 0, R.string.menu_about)
		.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	  switch (item.getItemId()) {
	    case SETTINGS_ID: {
	      Intent intent = new Intent(Intent.ACTION_VIEW);
	      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	      intent.setClassName(this, PreferencesActivity.class.getName());
	      startActivity(intent);
	      break;
	    }
	    case ABOUT_ID:
	      AlertDialog.Builder builder = new AlertDialog.Builder(this);
	      builder.setTitle(getString(R.string.title_about) + GlutenvrijActivity.VERSION_NAME);
	      builder.setMessage(getString(R.string.msg_about) + "\n" + getString(R.string.zxing_url));
	      builder.show();
	      break;
	  }
	  return super.onOptionsItemSelected(item);
	}
	
}