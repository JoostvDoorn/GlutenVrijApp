package com.joostvdoorn.glutenvrij;

import java.util.ArrayList;

public interface SearchObserver {
	public static ArrayList<SearchResult> CONNECTION_ERROR = new ArrayList<SearchResult>();

	public void notify(ArrayList<SearchResult> result);
}
