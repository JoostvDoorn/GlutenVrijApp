package com.joostvdoorn.glutenvrij;

import java.util.ArrayList;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.android.apps.analytics.easytracking.TrackedActivity;
import com.joostvdoorn.glutenvrij.scanner.PreferencesActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SearchActivity extends TrackedActivity implements SearchObserver, SpellCheckObserver, OnEditorActionListener {
	

	private static final int SETTINGS_ID = Menu.FIRST;
	private static final int ABOUT_ID = Menu.FIRST + 1;
	
	private ArrayList<SearchResult> results = new ArrayList<SearchResult>();
	private ListView list;
	private Search search; 
	private SpellCheck spellcheck;
	private String keyword = ""; //Current active keyword
	private int category = 0; //Current active category
	private boolean suggestion = false;

    /** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
        
        setContentView(R.layout.search);
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
        	keyword = extras.getString("com.joostvdoorn.glutenvrij.SearchValue");
        	category = Integer.parseInt(extras.getString("com.joostvdoorn.glutenvrij.SearchCategory"));
        	if(keyword == null) {
        		keyword = "";
        	}
        }

        list = (ListView) findViewById(R.id.searchResultsList);
        //Add the text to the search field and put the pointer to the end of the field
		EditText searchText = ((EditText) findViewById(R.id.searchText));
        searchText.setText(keyword);
        searchText.setSelection(keyword.length());
        searchText.setOnEditorActionListener(this);
        //Search for results
        final Object data = getLastNonConfigurationInstance();
        if(data == null) {
        	search = new Search();
        	search.execute(category,keyword,this);
    		keyword = ((EditText) findViewById(R.id.searchText)).getText().toString();
    		EasyTracker.getTracker().trackEvent("search","search",""+keyword, 0);
        }
        else {
        	notify((ArrayList<SearchResult>) data);
        	
        }
		suggestion = false;
    }
	@Override
	public void notify(ArrayList<SearchResult> result) {
		//Show the results in the list
		results = result;
		if(results.size()>0) {
			SearchAdapter searchAdapter = new SearchAdapter(this, R.layout.list_item, results);
	        list.setAdapter(searchAdapter);
	        list.setOnItemClickListener(searchAdapter);
			//Hide the searching text
			findViewById(R.id.searchingContainer).setVisibility(View.GONE);
			//Hide the cancel button
			findViewById(R.id.cancelContainer).setVisibility(View.GONE);
			//Show the list
			findViewById(R.id.searchResultsList).setVisibility(View.VISIBLE);
			findViewById(R.id.searchScreen).setBackgroundColor(Color.TRANSPARENT);
			//Hide the no results text
			findViewById(R.id.noResultsContainer).setVisibility(View.GONE);
			//Clear reference to search task
			search = null;
			suggestion = false;
		}
		else if(result == SearchObserver.CONNECTION_ERROR) {
			connectionProblem();
		}
		else {
			spellcheck = new SpellCheck();
			spellcheck.execute(keyword,this);
		}
	}
	public void search(View view) {

		//First see if the keyword is the same
		if(!keyword.equals(((EditText) findViewById(R.id.searchText)).getText().toString()) || search == null) {
			keyword = ((EditText) findViewById(R.id.searchText)).getText().toString();
			//Cancel previous search
			if(search != null) {
				search.cancel(true);
				search = null;
				EasyTracker.getTracker().trackEvent("search","correction",""+keyword, 0);
			}
			else {
				EasyTracker.getTracker().trackEvent("search","search",""+keyword, 0);
			}
			//Show the searching text
			findViewById(R.id.searchingContainer).setVisibility(View.VISIBLE);
			//Show the cancel button
			findViewById(R.id.cancelContainer).setVisibility(View.VISIBLE);
			//Hide the list
			findViewById(R.id.searchResultsList).setVisibility(View.GONE);
			findViewById(R.id.searchScreen).setBackgroundResource(R.drawable.background);
			//Hide the connection failed text
			findViewById(R.id.connectionErrorContainer).setVisibility(View.GONE);
			//Hide the no results text
			findViewById(R.id.noResultsContainer).setVisibility(View.GONE);
	        //Search for results
	        search = new Search();
	        search.execute(category,keyword,this);
			suggestion = false;
		}
	}
	public void cancel(View view) {
		keyword = ((EditText) findViewById(R.id.searchText)).getText().toString();
		EasyTracker.getTracker().trackEvent("search","cancel",""+keyword, 0);
		if(search != null) {
			search.cancel(true);
		}
		if(spellcheck != null) {
			spellcheck.cancel(true);
		}
		finish();
	}
	public void noResults() {
		keyword = ((EditText) findViewById(R.id.searchText)).getText().toString();
		EasyTracker.getTracker().trackEvent("search","empty",""+keyword, 0);
		//Hide the list
		findViewById(R.id.searchResultsList).setVisibility(View.GONE);
		findViewById(R.id.searchScreen).setBackgroundResource(R.drawable.background);
		//Hide the searching text
		findViewById(R.id.searchingContainer).setVisibility(View.GONE);
		//Hide the cancel button
		findViewById(R.id.cancelContainer).setVisibility(View.GONE);
		//Hide the connection failed text
		findViewById(R.id.connectionErrorContainer).setVisibility(View.GONE);
		//Show the no results text
		findViewById(R.id.noResultsContainer).setVisibility(View.VISIBLE);
	}
	public void connectionProblem() {
		//Hide the list
		findViewById(R.id.searchResultsList).setVisibility(View.GONE);
		findViewById(R.id.searchScreen).setBackgroundResource(R.drawable.background);
		//Hide the searching text
		findViewById(R.id.searchingContainer).setVisibility(View.GONE);
		//Hide the cancel button
		findViewById(R.id.cancelContainer).setVisibility(View.GONE);
		//Hide the no results text
		findViewById(R.id.noResultsContainer).setVisibility(View.GONE);
		//Show the connection failed text
		findViewById(R.id.connectionErrorContainer).setVisibility(View.VISIBLE);
	}
	@Override
	public void notifySpelling(ArrayList<ArrayList<String>> result) {
		if(result.size()>=1 && result.get(0).size()>=1 && suggestion == false) {
			//Try a search for a misspelled word
	        search = new Search(true);
	        search.execute(category,result.get(0).get(0),this);
	        suggestion = true;
		}
		else {
			//Could not find any results or spelling suggestions
			noResults();
		}

		//Clear reference to search task
		search = null;
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
	public Object onRetainNonConfigurationInstance() {
	    final ArrayList<SearchResult> resultsList = results;
	    return resultsList;
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