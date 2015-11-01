package com.joostvdoorn.glutenvrij;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchAdapter extends ArrayAdapter<SearchResult> implements OnItemClickListener {
	private LayoutInflater mInflater;
	private Context context;
	private ArrayList<SearchResult> result;

	public SearchAdapter(Context context, int textViewResourceId, List<SearchResult> objects) {
		super(context, textViewResourceId, objects);
		result = (ArrayList<SearchResult>) objects;
		mInflater = LayoutInflater.from(context);
		this.context = context;
	}
	


	public View getView(final int position, View convertView, ViewGroup parent) {
 
		// When convertView is not null, we can reuse it directly, there is
		// no need
		// to reinflate it. We only inflate a new View when the convertView
		// supplied
		// by ListView is null.
		View resultView;
		if(result.get(position).isSuggestion()==false) {
			if (convertView == null || convertView.getId()!=R.layout.list_item) {
				convertView = mInflater.inflate(R.layout.list_item, null);
			}
			switch(result.get(position).allowed(PreferenceManager.getDefaultSharedPreferences(context))) {
				case SearchResult.FREE_FROM: convertView.findViewById(R.id.container).setBackgroundResource(R.drawable.green_gradient); break;
				case SearchResult.NO_DATA: convertView.findViewById(R.id.container).setBackgroundResource(R.drawable.yellow_gradient); break;
				case SearchResult.CONTAINS: convertView.findViewById(R.id.container).setBackgroundResource(R.drawable.red_gradient); break;
			}
			((TextView) convertView.findViewById(R.id.productName)).setText(result.get(position).getName());
			String line = "";
			line += result.get(position).getStore() + " - ";
			line += result.get(position).getEan() + " - ";
			Resources res = convertView.getResources();
			switch(result.get(position).getLactoseInfo()) {
				case SearchResult.FREE_FROM:
					line += res.getString(R.string.lactosefree) + " - ";
					break;
				case SearchResult.CONTAINS:
					line += res.getString(R.string.lactose_contains) + " - ";
					break;
				case SearchResult.NO_DATA:
					line += res.getString(R.string.lactose_no_data) + " - ";
					break;
			}
			switch(result.get(position).getStarchInfo()) {
				case SearchResult.FREE_FROM:
					line += res.getString(R.string.starchfree);
					break;
				case SearchResult.CONTAINS:
					line += res.getString(R.string.starch_contains);
					break;
				case SearchResult.NO_DATA:
					line += res.getString(R.string.starch_no_data);
					break;
			}
			String bsmString = result.get(position).getBsmString();
			line +=  bsmString.length() > 0 ? " - " + bsmString : "";
			((TextView) convertView.findViewById(R.id.resultInfoLine)).setText(line);
			resultView = convertView;
		}
		else {
			View suggestionView = mInflater.inflate(R.layout.list_suggestion, null);
			Resources res = suggestionView.getResources();
			((TextView) suggestionView.findViewById(R.id.suggestion)).setText(res.getString(R.string.did_you_mean)+": "+result.get(position).getName());
			resultView = suggestionView;
		}
		//((TextView) convertView.findViewById(R.id.category)).setText(result.get(position).getCategory());
		return resultView;
	}
	public boolean isEnabled(int position) {
		return result.get(position).isSuggestion()==false;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//Launches the detailed view of the item
		Intent myIntent = new Intent(view.getContext(), SearchResultActivity.class);
		myIntent.putExtra("com.joostvdoorn.glutenvrij.SearchResult",result.get(position));
    	view.getContext().startActivity(myIntent);
	}
}
