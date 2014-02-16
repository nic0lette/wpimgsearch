
package org.hitori.demo.wikipediasearch;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;

import org.hitori.demo.wikipediasearch.ImageDownloader.ViewDownloadRequest;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SearchResultsListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private EditText mSearchTerm;
    private GridLayout mResultsGrid;
    private LayoutInflater mInflater;

    // Simplify the interface for managing search terms
    private SearchTermWatcher mSearchWatcher = new SearchTermWatcher();

    // Supply the ability to cache results
    private CachableSearch mCacheableSearch = new CachableSearch();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Save a reference to the layout inflater for later
        mInflater = LayoutInflater.from(this);

        mSearchTerm = (EditText) findViewById(R.id.search_et);
        mSearchTerm.addTextChangedListener(mSearchWatcher);

        mResultsGrid = (GridLayout) findViewById(R.id.results_gl);
    }

    /**
     * Callback from a serach being complete
     */
    @Override
    public void onSearchResults(String term, List<WikiPage> results) {
        // Simplify some later code
        final int resultsCount = (results == null) ? 0 : results.size();

        // List of images (and associated views) to download
        final List<ViewDownloadRequest> downloads = new ArrayList<ViewDownloadRequest>();

        // Current indexes
        int currentResult = 0;
        int currentChild = 0;

        // Aliases for easily comparing pages in the results
        WikiPage curRes = (currentResult < resultsCount) ? results.get(currentResult) : null;
        WikiPage curChild = (currentChild < mResultsGrid.getChildCount()) ? (WikiPage) mResultsGrid
                .getChildAt(currentChild).getTag()
                : null;

        while (curRes != null || curChild != null) {
            if (curChild == null && curRes != null) {
                // Add the result
                final View view = viewFromPage(curRes);
                mResultsGrid.addView(view);
                Log.v(LOG_TAG, "Add page: " + curRes.title);

                // Download to perform?
                if (curRes.thumbnailUrl != null) {
                    downloads.add(new ViewDownloadRequest(curRes.thumbnailUrl, view));
                }

                // Next result and bump the child index
                ++currentResult;
                ++currentChild;
            } else if (curChild != null && curRes == null) {
                // Remove the child, since it's no longer in the results
                mResultsGrid.removeViewAt(currentChild);
                Log.v(LOG_TAG, "Remove child: " + curChild.title);

                // No need to change the current child, since we deleted a
                // view (so the current index is the same, but it now points
                // to the previously "next" child
            } else if (curChild == null && curRes == null) {
                // Ran out of both lists! We're done!
                Log.v(LOG_TAG, "Update complete!");
            } else {
                if (curRes.title.compareTo(curChild.title) < 0) {
                    // Add the result
                    final View view = viewFromPage(curRes);
                    mResultsGrid.addView(view, currentChild);
                    Log.v(LOG_TAG, "Insert page: " + curRes.title);

                    // Download to perform?
                    if (curRes.thumbnailUrl != null) {
                        downloads.add(new ViewDownloadRequest(curRes.thumbnailUrl, view));
                    }

                    // Next result
                    ++currentResult;

                    // Also next child, because we just bumped it forward
                    ++currentChild;

                    // Done (for this round)
                    break;
                } else if (curRes.title.compareTo(curChild.title) > 0) {
                    // Remove a child, since it doesn't belong in the
                    // results
                    mResultsGrid.removeViewAt(currentChild);
                    Log.v(LOG_TAG, "Remove child: " + curChild.title);
                } else {
                    // The child is in the results, so it stays -- just continue
                    Log.v(LOG_TAG, "Skipping already included child: " + curChild.title);
                    ++currentResult;
                    ++currentChild;
                }
            }

            // Get the next pages to work with
            curRes = (results != null && currentResult < results.size()) ? results
                    .get(currentResult) : null;
            curChild = (currentChild < mResultsGrid.getChildCount()) ? (WikiPage) mResultsGrid
                    .getChildAt(currentChild).getTag()
                    : null;
        }

        // Start the download of images
        new ImageDownloader(this).execute(downloads.toArray(new ViewDownloadRequest[downloads
                .size()]));
    }

    /*
     * Helper method to construct a view from a WikiPage
     */
    private View viewFromPage(final WikiPage page) {
        final View view = mInflater.inflate(R.layout.page_image, null);
        ((TextView) (view.findViewById(R.id.page_tv))).setText(page.title);
        view.setTag(page);

        final Uri uri = Uri.parse(page.fullurl);
        if (uri != null) {
            // Connect a very basic touch handler
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                    browseIntent.setData(uri);
                    startActivity(browseIntent);
                }
            });
        }

        return view;
    }

    /*
     * Internal interface method from SearchTermWatcher
     */
    private void onNewSearchTerm(final String term) {
        mCacheableSearch.performSearch(term, this);
    }

    /*
     * TextWatcher implementation
     */
    private class SearchTermWatcher implements TextWatcher {
        /*
         * Auto-correct will manipulate the text as it's entered and we'll get
         * multiple callbacks even though the text is the same. This helps
         * prevent those extra calls
         */
        private String prevTerm = null;

        @Override
        public void afterTextChanged(final Editable e) {
            // Call toString() just once...
            final String s = e.toString();
            if (s != null && !s.equals(prevTerm)) {
                MainActivity.this.onNewSearchTerm(s);
                prevTerm = s;
            }
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                final int after) {
            // Nothing special to do
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                final int count) {
            // Nothing special to do
        }
    }
}
