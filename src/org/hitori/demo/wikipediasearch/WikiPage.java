
package org.hitori.demo.wikipediasearch;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Java Object representation of some details of a Wikipedia page
 * @author niya
 *
 */
public class WikiPage {
    private static final String LOG_TAG = WikiPage.class.getSimpleName();
    
    public final long pageId;
    public final String title;
    public final String fullurl;
    public final String thumbnailUrl;

    private WikiPage(final long pageId, final String title, final String fullurl,
            final String thumbnailUrl) {
        this.pageId = pageId;
        this.title = title;
        this.fullurl = fullurl;
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Creates a WikiPage object from a JSONObject
     * @param json The JSONObject representation of the WikiPage
     * @return A WikiPage representing details of the page from the JSON object or null if a parse error occurred
     */
    public static WikiPage fromJson(final JSONObject json) {
        try {
            final String title = json.has("title") ? json.getString("title") : "";
            final String fullurl = json.has("fullurl") ? json.getString("fullurl") : null;
            String thumbnailUrl = null;

            if (json.has("thumbnail")) {
                final JSONObject thumbnailObj = json.getJSONObject("thumbnail");
                thumbnailUrl = (thumbnailObj != null && thumbnailObj.has("source")) ? thumbnailObj
                        .getString("source") : null;
            }
            return new WikiPage(0, title, fullurl, thumbnailUrl);
        } catch (final JSONException e) {
            Log.e(LOG_TAG, "JSON Parse Exception", e);
        }
        
        // Couldn't create a page
        return null;
    }
}
