package com.joostvdoorn.glutenvrij;

import java.util.ArrayList;

public interface SpellCheckObserver {

	public void notifySpelling(ArrayList<ArrayList<String>> result);

}
