package com.joostvdoorn.glutenvrij;

import com.google.android.apps.analytics.easytracking.TrackedActivity;
import com.joostvdoorn.glutenvrij.scanner.PreferencesActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

public class SearchResultActivity extends TrackedActivity {
	
	private static final int SETTINGS_ID = Menu.FIRST;
	private static final int ABOUT_ID = Menu.FIRST + 1;

	private SearchResult result;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.result);
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
        	result = extras.getParcelable("com.joostvdoorn.glutenvrij.SearchResult");
        }
        //Set all text in the results
        TextView productName = ((TextView) findViewById(R.id.productName));
        productName.setText(result.getName());
        Resources res = getResources();
        ((TextView) findViewById(R.id.ean)).setText(result.getEan().equals("?") ? res.getString(R.string.unknown) : result.getEan());
        ((TextView) findViewById(R.id.store)).setText(result.getStore());
        ((TextView) findViewById(R.id.category)).setText(result.getCategory());
        String lactose_result = "?";
        String starch_result = "?";
		switch(result.getLactoseInfo()) {
			case SearchResult.FREE_FROM:
				lactose_result = res.getString(R.string.lactosefree);
				break;
			case SearchResult.CONTAINS:
				lactose_result = res.getString(R.string.lactose_contains);
				break;
			case SearchResult.NO_DATA:
				lactose_result = res.getString(R.string.lactose_no_data);
				break;
		}
		switch(result.getStarchInfo()) {
			case SearchResult.FREE_FROM:
				starch_result = res.getString(R.string.starchfree);
				break;
			case SearchResult.CONTAINS:
				starch_result = res.getString(R.string.starch_contains);
				break;
			case SearchResult.NO_DATA:
				starch_result = res.getString(R.string.starch_no_data);
				break;
		}
        ((TextView) findViewById(R.id.lactose)).setText(lactose_result);
        ((TextView) findViewById(R.id.starch)).setText(starch_result);
        String[] contaminationRisk = res.getStringArray(R.array.contamination_risk);
        ((TextView) findViewById(R.id.contamination)).setText(contaminationRisk[result.getBsm()-1]);
        String[] source = res.getStringArray(R.array.source);
        ((TextView) findViewById(R.id.dlc)).setText(res.getString(R.string.last_updated)+" "+result.getDLCString()+". "+source[result.getSource()-1]);
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
