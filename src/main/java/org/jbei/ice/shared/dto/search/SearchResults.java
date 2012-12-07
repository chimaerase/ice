package org.jbei.ice.shared.dto.search;

import java.util.LinkedList;

import org.jbei.ice.shared.dto.IDTOModel;

/**
 * Wrapper around a list of search results which also contains information about the search.
 * Information such as query, result count
 *
 * @author Hector Plahar
 */
public class SearchResults implements IDTOModel {

    private long resultCount;
    private LinkedList<SearchResultInfo> resultInfos;
    private SearchQuery query;

    public SearchResults() {
    }

    public LinkedList<SearchResultInfo> getResults() {
        return this.resultInfos;
    }

    public void setResults(LinkedList<SearchResultInfo> results) {
        this.resultInfos = results;
    }

    public void setResultCount(long count) {
        this.resultCount = count;
    }

    /**
     * @return total query result count. not just the count of results returned
     */
    public long getResultCount() {
        return this.resultCount;
    }

    public SearchQuery getQuery() {
        return query;
    }

    public void setQuery(SearchQuery query) {
        this.query = query;
    }
}
