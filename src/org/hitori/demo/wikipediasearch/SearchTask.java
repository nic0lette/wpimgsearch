
package org.hitori.demo.wikipediasearch;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchTask extends AsyncTask<Void, Void, List<WikiPage>> {
    private static final String LOG_TAG = SearchTask.class.getSimpleName();
    
    /*
     * URI to the Wikipedia API (with default arguments set)
     */
    private static final String SEARCH_URI = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages%%7Cinfo&format=json&piprop=thumbnail&inprop=url&pilimit=50&generator=allpages&gaplimit=50&pithumbsize=96&gapprefix=%s";
    
    /*
     * User-agent for our API requests
     */
    private static final String USER_AGENT = "WikipediaImageSearch/0.1 (https://github.com/niyafox/wpimgsearch; nicole AT hitori DOT org)";

    /*
     * Search string for this task
     */
    private final String mSearchTerm;

    private final SearchResultsListener mListener;

    public SearchTask(final SearchResultsListener listener, final String searchTerm) {
        String encodedTerm = null;
        try {
            encodedTerm = URLEncoder.encode(searchTerm, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // I don't even know what to do in this case >_>
        }

        mSearchTerm = encodedTerm;
        mListener = listener;
    }
    
    protected List<WikiPage> parseResult(final String result) {
        try {
            final JSONObject obj = new JSONObject(result);
            if (obj.has("query")) {
                final JSONObject query = obj.getJSONObject("query");
                if (query != null && query.has("pages")) {
                    return parsePages(query.getJSONObject("pages"));
                }
            }
        } catch (final JSONException e) {
            Log.e(LOG_TAG, "Failed parsing results", e);
            return null;
        }
        
        // No pages?
        return new ArrayList<WikiPage>(0);
    }
    
    protected List<WikiPage> parsePages(final JSONObject pages) {
        final List<WikiPage> pagesList = new ArrayList<WikiPage>();
        
        @SuppressWarnings("unchecked")
        final Iterator<String> keys = pages.keys();
        
        while (keys.hasNext()) {
            final String key = keys.next();
            try {
                final JSONObject page = pages.getJSONObject(key);
                final String title = page.has("title") ? page.getString("title") : null;
                final String fullurl = page.has("fullurl") ? page.getString("fullurl") : null;
                String thumbnailUrl = null;
                
                if (page.has("thumbnail")) {
                    final JSONObject thumbnailObj = page.getJSONObject("thumbnail");
                    thumbnailUrl = (thumbnailObj != null && thumbnailObj.has("source")) ? thumbnailObj.getString("source") : null;
                }
                pagesList.add(new WikiPage(Long.parseLong(key), title, fullurl, thumbnailUrl));
            } catch (final JSONException e) {
                // Failed to parse that page but try our best to continue
                Log.e(LOG_TAG, "Parse Exception", e);
            }
        }
        
        // Send back the list of pages
        return pagesList;
    }

    /*
     * Code based on
     * http://stackoverflow.com/questions/4457492/simple-http-client
     * -example-in-android
     */

    @Override
    protected List<WikiPage> doInBackground(Void... params) {
        List<WikiPage> results = null;
        
        // Get the default client
        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpGet httpget = new HttpGet(String.format(SEARCH_URI, mSearchTerm));
        httpget.setHeader("User-Agent", USER_AGENT);

        // Execute the request
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);

            // Get hold of the response entity
            final HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release

            if (entity != null) {

                // A Simple JSON Response Read
                final InputStream instream = entity.getContent();
                final String result = convertStreamToString(instream);
                results = parseResult(result);

                // now you have the string representation of the HTML request
                instream.close();
            }
        } catch (final Exception e) {
            Log.e(LOG_TAG, "API call failed", e);
            return null;
        }

        // Send the results back to the UI thread to be used
        return results;
    }

    @Override
    protected void onPostExecute(List<WikiPage> result) {
        mListener.onSearchResults(mSearchTerm, result);
    }

    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         */
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
