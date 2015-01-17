package com.joostvdoorn.glutenvrij;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

public class Search extends AsyncTask<Object, String, ArrayList<SearchResult>> {

	private HttpClient httpClient = new DefaultHttpClient();
	private HttpUriRequest request = null;
	private SearchObserver observer;
	private boolean suggestion = false;
	
	public Search() {
		super();
	}
	public Search(boolean suggestion) {
		super();
		this.suggestion = suggestion;
	}
	public boolean isSuggestion() {
		return suggestion;
	}
	
	public ArrayList<SearchResult> search(int category, String keyword) throws URISyntaxException, IOException {
		ArrayList<SearchResult> result = new ArrayList<SearchResult>();
        request = new HttpGet(new URI("http://www.joostvandoorn.com/glutenvrij/search.php?opZoeken="+category+"&zoektermen="+URLEncoder.encode(keyword)));
		HttpEntity response = null;
		response = httpClient.execute(request).getEntity();
		if(response.getContentType().getValue().equals("application/x-json")) {
			BufferedReader inputReader = null;
			inputReader = new BufferedReader(new InputStreamReader(response.getContent()));
			String resultString = inputReader.readLine();
			JSONObject searchResult = null;
			try {
				searchResult = new JSONObject(resultString);
				JSONArray resultArray = searchResult.getJSONArray("results");
				//Found a result and using the spell checker to fix the word, display the word used to fix it
				if(this.suggestion == true && resultArray.length()>0) {
					result.add(new SearchResult(keyword));
				}
				for(int i = 0; i < resultArray.length() && i < 1000; i++) {
					result.add(new SearchResult((String) resultArray.getJSONArray(i).get(0)
							,(String) resultArray.getJSONArray(i).get(1)
							,(String) resultArray.getJSONArray(i).get(2)
							,(String) resultArray.getJSONArray(i).get(3)
							,(Integer) resultArray.getJSONArray(i).get(4)
							,(Integer) resultArray.getJSONArray(i).get(5)
							,(String) resultArray.getJSONArray(i).get(6)
							,(String) resultArray.getJSONArray(i).get(7)
							,(String) resultArray.getJSONArray(i).get(8)
							));

				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	protected ArrayList<SearchResult> doInBackground(Object... parameters) {
		ArrayList<SearchResult> result = new ArrayList<SearchResult>();
		observer = (SearchObserver) parameters[2];
		try {
			result = search((Integer) parameters[0], (String) parameters[1]);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			result = SearchObserver.CONNECTION_ERROR;
		}
		return result;
	}
    @Override
    protected void onPostExecute(ArrayList<SearchResult> result) {
        super.onPostExecute(result);
        observer.notify(result);
        httpClient = null;
    }
    @Override
    protected void onCancelled() {
    	//Properly closes the socket connection to avoid memory leaks
    	request.abort();
    	request = null;
        httpClient = null;
    }
}
