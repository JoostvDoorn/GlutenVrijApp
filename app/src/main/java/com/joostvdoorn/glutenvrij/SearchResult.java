package com.joostvdoorn.glutenvrij;

import java.util.Calendar;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.joostvdoorn.glutenvrij.scanner.PreferencesActivity;

public class SearchResult implements Parcelable {
	
	public static final int NO_DATA = 3;
	public static final int CONTAINS = 2;
	public static final int FREE_FROM = 1;
	
	public static final int ALLOWED = 1;
	public static final int NOT_ALLOWED = 2;
	
	public static final int BSM_BG0 = 1;
	public static final int BSM_NG = 2;
	public static final int BSM_MGD = 3;
	public static final int BSM_BG1 = 4;
	public static final int BSM_GA = 5;
	public static final int BSM_UNKNOWN = 6;
	public static final int BSM_BG2 = 7;
	public static final int BSM_BG3 = 8;
	public static final int BSM_BG4 = 9;
	public static final int BSM_BL1 = 10;
	
	public static final int SOURCE_UNKNOWN = 1;
	public static final int SOURCE_EMAIL = 2;
	public static final int SOURCE_FAX = 3;
	public static final int SOURCE_LIST = 4;
	public static final int SOURCE_TELEPHONE = 5;
	public static final int SOURCE_PACKAGE = 6;
	public static final int SOURCE_WEBSITE = 7;
	public static final int SOURCE_NEWSGROUP = 8;
	
	private String name;
	private String ean;
	private String store;
	private String category;
	private int starchInfo;
	private int lactoseInfo;
	private int bsm;
	private int dlc;
	private int source;
	private boolean suggestion = false;
	
	public SearchResult(String name) {
		this.name = name;
		this.suggestion = true;
	}
	public SearchResult(String name, String ean, String store, String category, int starchInfo, int lactoseInfo, String bsm, String dlc, String source) {
		this.name = name;
		this.ean = ean;
		this.store = store;
		this.category = category;
		this.starchInfo = starchInfo;
		this.lactoseInfo = lactoseInfo;
		this.bsm = bsmToInt(bsm);
		this.dlc = Integer.parseInt(dlc);
		this.source = sourceToInt(source);
	}
	public String getName() {
		return name;
	}
	public String getEan() {
		return ean;
	}
	public String getStore() {
		return store;
	}
	public String getCategory() {
		return category;
	}
	public int getStarchInfo() {
		return starchInfo;
	}
	public int getLactoseInfo() {
		return lactoseInfo;
	}
	public int getBsm() {
		return bsm;
	}
	public int getDLC() {
		return dlc;
	}
	public String getDLCString() {
		int year = 0;
		int yearLastDigit = Math.round(dlc/100);
		int month = dlc%100;
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		int currentYearLastDigit = currentYear%10;
		if(currentYearLastDigit<yearLastDigit) {
			year += -10;
		}
		year += currentYear-currentYearLastDigit+yearLastDigit;
		return DateUtils.getMonthString(month-1,DateUtils.LENGTH_LONG)+" "+year;
	}
	public int getSource() {
		return source;
	}
	private int bsmToInt(String bsm) {
		if(bsm.equals("BG0")) {
			return BSM_BG0;
		}
		else if(bsm.equals("BG1")) {
			return BSM_BG1;
		}
		else if(bsm.equals("BG2")) {
			return BSM_BG2;
		}
		else if(bsm.equals("BG3")) {
			return BSM_BG3;
		}
		else if(bsm.equals("BG4")) {
			return BSM_BG4;
		}
		else if(bsm.equals("BL1")) {
			return BSM_BL1;
		}
		else if(bsm.equals("NG")) {
			return BSM_NG;
		}
		else if(bsm.equals("MGD")) {
			return BSM_MGD;
		}
		else if(bsm.equals("GA")) {
			return BSM_GA;
		}
		else {
			return BSM_UNKNOWN;
		}
	}
	private int sourceToInt(String source) {

		if(source.equals("E")) {
			return SOURCE_EMAIL;
		}
		else if(source.equals("F")) {
			return SOURCE_FAX;
		}
		else if(source.equals("L")) {
			return SOURCE_LIST;
		}
		else if(source.equals("T")) {
			return SOURCE_TELEPHONE;
		}
		else if(source.equals("V")) {
			return SOURCE_PACKAGE;
		}
		else if(source.equals("W")) {
			return SOURCE_WEBSITE;
		}
		else if(source.equals("Y")) {
			return SOURCE_NEWSGROUP;
		}
		else {
			return SOURCE_UNKNOWN;
		}
	}
	/**
	 * This function returns 1 if the user is allowed to eat it based on the preferences
	 * 3 if there is no data
	 * 2 if it's not allowed
	 */
	public int allowed(SharedPreferences prefs) {
		if((!prefs.getBoolean(PreferencesActivity.KEY_STARCH, false) || getStarchInfo() == FREE_FROM)
		&&
		(!prefs.getBoolean(PreferencesActivity.KEY_LACTOSE, false) || getLactoseInfo() == FREE_FROM))
		{		//allowed to eat it || free from
			return ALLOWED;
		}
		else if((prefs.getBoolean(PreferencesActivity.KEY_STARCH, false) && getStarchInfo() == CONTAINS)
				||
				(prefs.getBoolean(PreferencesActivity.KEY_LACTOSE, false) && getLactoseInfo() == CONTAINS))
		{
			return NOT_ALLOWED;
		}
		else {
			return NO_DATA;
		}
	}
	public String toString() {
		return getName();
	}
	public boolean isSuggestion() {
		return suggestion;
	}

    /* everything below here is for implementing Parcelable */
    private SearchResult(Parcel in) {
       	name = in.readString();
       	ean = in.readString();
       	store = in.readString();
        category = in.readString();
        starchInfo = in.readInt();
        lactoseInfo = in.readInt();
        bsm = in.readInt();
        dlc = in.readInt();
        source = in.readInt();
        boolean[] suggestions = new boolean[1];
       	in.readBooleanArray(suggestions);
       	suggestion = suggestions[0];
    }

	@Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(ean);
		out.writeString(store);
		out.writeString(category);
		out.writeInt(starchInfo);
		out.writeInt(lactoseInfo);
		out.writeInt(bsm);
		out.writeInt(dlc);
		out.writeInt(source);
		out.writeBooleanArray(new boolean[] {true});
    }
	
    public int describeContents() {
        return 0;
    }


    // this is used to regenerate the object
    public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

}
