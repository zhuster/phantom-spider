package com.csdc.spider.exception;


import com.csdc.spider.enumeration.error.SearchError;

/**
 * @author zhangzhi
 * @since <pre>2019/6/8</pre>
 */
public class SearchingException extends RuntimeException {

    private static final long serialVersionUID = 3228937799554051013L;
    private SearchError searchError;

    public SearchingException() {
    }

    public SearchingException(SearchError searchError) {
        super(searchError.toString());
        this.searchError = searchError;
    }

    public SearchError getSearchError() {
        return searchError;
    }
}
