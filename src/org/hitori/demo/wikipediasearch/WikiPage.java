package org.hitori.demo.wikipediasearch;

public class WikiPage {
    public final long pageId;
    public final String title;
    public final String fullurl;
    public final String thumbnailUrl;
    
    public WikiPage(final long pageId, final String title, final String fullurl, final String thumbnailUrl) {
        this.pageId = pageId;
        this.title = title;
        this.fullurl = fullurl;
        this.thumbnailUrl = thumbnailUrl;
    }
}
