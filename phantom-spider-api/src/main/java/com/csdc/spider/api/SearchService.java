package com.csdc.spider.api;

import com.csdc.spider.enumeration.SearchContentType;
import com.csdc.spider.model.AdvancedSearchCondition;
import com.csdc.spider.model.Paper;
import com.csdc.spider.model.SearchResult;
import org.apache.http.HttpResponse;

/**
 * 知网检索扒取文章信息接口
 *
 * @author zhangzhi
 * @since <pre>2019/6/1</pre>
 */
public interface SearchService {
    /**
     * 初级检索
     *
     * @param searchContentType
     * @param content
     */
    SearchResult simpleSearch(SearchContentType searchContentType, String content);

    /**
     * 获取论文详情
     *
     * @param link
     */
    Paper findPaperInfo(String link);


    /**
     * 根据页面链接获取单页面的论文
     *
     * @param pageLink
     */
    SearchResult getEntriesByPageLink(String pageLink);

    /**
     * 高级检索
     *
     * @param condition
     */
    SearchResult advancedSearch(AdvancedSearchCondition condition);

    /**
     * 根据链接下载
     *
     * @param downloadLink
     */
    HttpResponse getDownloadResponse(String downloadLink);
}
