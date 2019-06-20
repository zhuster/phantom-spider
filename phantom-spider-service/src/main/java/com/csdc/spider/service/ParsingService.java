package com.csdc.spider.service;

import com.csdc.spider.enumeration.error.SearchError;
import com.csdc.spider.exception.SearchingException;
import com.csdc.spider.model.Entry;
import com.csdc.spider.model.SearchResult;
import com.csdc.spider.util.CNKIConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author zhangzhi
 * @since <pre>2019/6/18</pre>
 */
@Service
@Slf4j
public class ParsingService {

    @Autowired
    CommonService commonService;

    /**
     * 解析html页面
     *
     * @param html
     */
    public SearchResult parseHtml(String html) {
        Document doc = Jsoup.parse(html);
        SearchResult searchResult = new SearchResult();
        Elements trs;
        try {
            Elements content = doc.getElementsByClass("GridTableContent");
            Element pagerTitle = doc.getElementById("lbPagerTitle");
            String rawNum = pagerTitle.parent().ownText();
            int total = specifyTotalNum(rawNum);
            searchResult.setTotal(total);
            Element table = content.get(0);
            Element child = table.child(0);
            trs = child.getElementsByAttribute("bgcolor");
        } catch (Exception e) {
            throw new SearchingException(SearchError.COOKIE_HAS_EXPIRED);
        }
        List<Entry> entries = null;
        try {
            entries = commonService.extractPaperEntry(trs);
        } catch (Exception e) {
            log.info("解析html页面失败");
            e.printStackTrace();
        }
        searchResult.setEntries(entries);
        Optional<Element> prev = Optional.ofNullable(doc.getElementById("Page_prev"));
        prev.ifPresent(e -> {
            String link = e.attr("href");
            if (!link.contains(CNKIConsts.PAGE_REFERER)) {
                link = CNKIConsts.PAGE_REFERER + link;
            }
            searchResult.setPrevPageLink(link);
        });
        Optional<Element> next = Optional.ofNullable(doc.getElementById("Page_next"));
        next.ifPresent(e -> {
            String link = e.attr("href");
            if (!link.contains(CNKIConsts.PAGE_REFERER)) {
                link = CNKIConsts.PAGE_REFERER + link;
            }
            searchResult.setNextPageLink(link);
        });
        return searchResult;
    }

    /**
     * 提取查询到的总条数信息
     *
     * @param rawNum 含有总条数信息的文本
     */
    public int specifyTotalNum(String rawNum) {
        if (StringUtils.isEmpty(rawNum)) {
            throw new SearchingException(SearchError.NO_PAPERS);
        }
        String num = rawNum.replaceAll("[^0-9]", "");
        int total = Integer.parseInt(num);
        return total;
    }

}
