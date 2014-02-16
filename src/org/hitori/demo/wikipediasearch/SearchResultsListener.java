package org.hitori.demo.wikipediasearch;

import java.util.List;

public interface SearchResultsListener {

    /**
     * Callback for when search results are available
     * @param term Search term used for the results
     * @param results Search results (including an empty list) or null if an error occurred
     */
    public void onSearchResults(String term, List<WikiPage> results);
}
