
package org.hitori.demo.wikipediasearch;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachableSearch implements SearchResultsListener {
    // Cache for storing results
    private final Map<String, SoftReference<List<WikiPage>>> mCachedResults;

    // As an object so we can synchronize it (to be safe)
    private Boolean mActiveSearch = false;
    
    // Current search's listener
    private SearchResultsListener mSearchResultsListener;
    
    // Pending search term - only one pending term is supported
    private String mPendingSearch;
    private SearchResultsListener mPendingListener;
    
    public CachableSearch() {
        mCachedResults = new HashMap<String, SoftReference<List<WikiPage>>>();
    }
    
    /**
     * Requests a search to be performed, utilizing cached results if they're available
     * @param term The term to search for
     * @param listener Callback for when results are available
     */
    public void performSearch(final String term, final SearchResultsListener listener) {
        // If the results for this search are cached, return them now
        final SoftReference<List<WikiPage>> cached = mCachedResults.get(term);
        if (cached != null && cached.get() != null) {
            // Results found, so we'll return them
            if (listener != null) {
                listener.onSearchResults(term, cached.get());
            }
            
            // Don't cancel any active search, but don't callback the results either
            if (mActiveSearch) {
                mSearchResultsListener = null;
            }
            mPendingListener = null;
        } else {
            /*
             * There is a design decision here both to simplify the interaction between the UI thread
             * and the worker, and also in a (perhaps silly) attempt to place less load on the Wikipedia servers.
             * 
             * Basically, if there's an active search currently, we won't start a new search, but will wait
             * for the current search to complete.  (We won't attempt to cancel it either)
             * 
             */
            synchronized (mActiveSearch) {
                if (mActiveSearch) {
                    // Save the details and leave the search pending
                    mPendingListener = listener;
                    mPendingSearch = term;
                } else {
                    // All set - perform the search
                    mActiveSearch = true;
                    mSearchResultsListener = listener;
                    new SearchTask(this, term).execute((Void[])null);
                }
            }
        }
    }

    @Override
    public void onSearchResults(final String term, final List<WikiPage> results) {
        // First, cache the results, if they're useful
        if (results != null) {
            mCachedResults.put(term, new SoftReference<List<WikiPage>>(results));
        }
        
        // Save the current listener so we can reset or clear it
        final SearchResultsListener listener = mSearchResultsListener;
        
        // Manage chaining of requests
        synchronized (mActiveSearch) {
            // Current search ended
            mActiveSearch = false;
            
            // Pending search?  Perform it now
            if (mPendingSearch != null) {
                performSearch(mPendingSearch, mPendingListener);
            } else {
                mActiveSearch = false;
                mSearchResultsListener = null;
            }
        }
        
        // Pass along the results for the search we just completed
        if (listener != null) {
            listener.onSearchResults(term, results);
        }
    }

}
