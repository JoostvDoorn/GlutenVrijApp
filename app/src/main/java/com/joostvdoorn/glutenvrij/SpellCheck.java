package com.joostvdoorn.glutenvrij;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.methods.HttpPost;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.AsyncTask;

public class SpellCheck extends AsyncTask<Object, String, ArrayList<ArrayList<String>>> { 

		private HttpPost request = null;
		private SpellCheckObserver observer;
		
		public ArrayList<ArrayList<String>> search(String keyword) throws URISyntaxException, IOException {
			//Format the XML that needs to be send to Google Spell
			//Checker.
			StringBuffer requestXML = new StringBuffer();
			requestXML.append("<spellrequest textalreadyclipped=\"0\""+
			" ignoredups=\"1\""+
			" ignoredigits=\"1\" ignoreallcaps=\"0\"><text>");
			requestXML.append(keyword);
			requestXML.append("</text></spellrequest>");
			//The Google Spell Checker URL
			URL url = new URL("https://www.google.com/tbproxy/spell?lang=nl&hl=nl");
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);

			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
			out.write(requestXML.toString());
			out.close();

			//Get the result from Google Spell Checker
			InputStream in = conn.getInputStream();
			/*BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String inputLine;
			String result = "";
			while ((inputLine = br.readLine()) != null) {
			 	result += inputLine;
			}*/
			SAXParser parser;
			SuggestionHandler handler = new SuggestionHandler();
			try {
				parser = SAXParserFactory.newInstance().newSAXParser();
				parser.parse(in, handler);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ArrayList<ArrayList<String>> suggestions = handler.getSuggestions();

			in.close();
			return suggestions;
		}

		@Override
		protected ArrayList<ArrayList<String>> doInBackground(Object... parameters) {
			ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
			observer = (SpellCheckObserver) parameters[1];
			try {
				result = search((String) parameters[0]);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
		}
	    @Override
	    protected void onPostExecute(ArrayList<ArrayList<String>> result) {
	        super.onPostExecute(result);
	        observer.notifySpelling(result);
	    }
	    @Override
	    protected void onCancelled() {
	    	//Properly closes the socket connection to avoid memory leaks
	    	if(request != null) {
		    	request.abort();
		    	request = null;
	    	}
	    }
	    public class SuggestionHandler extends DefaultHandler {
	    	private ArrayList<String> tempA = new ArrayList<String>();
	    	private ArrayList<ArrayList<String>> suggestions = new ArrayList<ArrayList<String>>();
	    	
	    	public ArrayList<ArrayList<String>> getSuggestions() {
	    		return suggestions;
	    	}
	    	@Override
	    	public void characters(char[] ch, int start, int length) throws SAXException {
	    		List<String> arr = Arrays.asList((new String(ch)).split("\t"));
	    		for(String value : arr) {
	    			tempA.add(value);
	    		}
	    	}
	    	@Override
	    	public void endElement(String uri, String localName, String qName) throws SAXException {
	    		if(qName.equals("c")) {
	    			suggestions.add(tempA);
	    		}
	    		tempA = new ArrayList<String>();
	    	}
	    }
	}
