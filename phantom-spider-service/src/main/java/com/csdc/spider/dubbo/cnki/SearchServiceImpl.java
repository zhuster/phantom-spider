package com.csdc.spider.dubbo.cnki;


import com.csdc.spider.api.SearchService;
import com.csdc.spider.enumeration.FileType;
import com.csdc.spider.enumeration.SearchContentType;
import com.csdc.spider.enumeration.error.SearchError;
import com.csdc.spider.exception.SearchingException;
import com.csdc.spider.model.AdvancedSearchCondition;
import com.csdc.spider.model.Binding;
import com.csdc.spider.model.Paper;
import com.csdc.spider.model.SearchResult;
import com.csdc.spider.service.CommonService;
import com.csdc.spider.service.ParsingService;
import com.csdc.spider.util.CNKIConsts;
import com.csdc.spider.util.ConfigConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 知网检索与条件检索服务端实现
 * Note：在消费端调用服务端接口方法的时候入参必须进行充分验证，服务端不再进行验证
 *
 * @author zhangzhi
 * @since <pre>2019/5/29</pre>
 */
@Slf4j
@org.apache.dubbo.config.annotation.Service(group = "cnki", version = "1.0.1")
@Service
public class SearchServiceImpl implements SearchService {


    private static ConcurrentHashMap<Integer, CookieStore> cookieCache = new ConcurrentHashMap<>(256);
    private static ConcurrentHashMap<Integer, String> referLink = new ConcurrentHashMap<>(256);
    private CloseableHttpClient client = null;

    @Autowired
    CommonService commonService;
    @Autowired
    ParsingService parsingService;

    static {
        System.setProperty("webdriver.chrome.driver", ConfigConsts.CHROME_DRIVER_LOCATION);
    }


    /**
     * 调用知网的初级检索，根据检索内容与内容种类驱动检索并提取检索内容
     * Note:注意异常调用<code>quit</code>方法，遇到过虚拟机(mem:2G)上oom的情况
     *
     * @param searchContentType 输入内容类型
     * @param content           输入内容
     */
    @Override
    public SearchResult simpleSearch(SearchContentType searchContentType, String content, int accountId) {
        ChromeDriver driver = new ChromeDriver();
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.pageLoadTimeout(10, TimeUnit.SECONDS);
        timeouts.implicitlyWait(50, TimeUnit.MILLISECONDS);
        driver.get(CNKIConsts.CNKI);
        Optional<SearchContentType> contentType = Optional.ofNullable(searchContentType);
        contentType.map(SearchContentType::getType).ifPresent(e -> {
            driver.executeScript("document.getElementById('DBFieldList').style.display='block';");
            driver.findElementByLinkText(e).click();
            log.info("查询类型：{} 内容：{}", e, content);
        });
        driver.findElementById("txt_SearchText").sendKeys(content);
        driver.findElementByClassName("search-btn").click();
        SearchResult searchResult = null;
        try {
            searchResult = commonService.extractSearchResult(driver);
        } catch (Exception e) {
            log.error("解析查询结果失败");
            e.printStackTrace();
        } finally {
            commonService.handleCookie(driver, cookieCache, accountId);
            driver.quit();
        }
        if (Objects.isNull(searchContentType)) {
            log.info("进行知网初级检索，查询类型：主题 查询内容：{}", content);
        }
        return searchResult;
    }

    /**
     * 根据初次扒取到的知网链接获取到相关文章详情信息 e.g.文章的摘要信息、caj&pdf下载链接等
     *
     * @param link
     */
    @Override
    public Paper findPaperInfo(String link) {
        try {
            link = URLDecoder.decode(link, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("解析论文详情链接失败");
            e.printStackTrace();
        }
        final Paper paper = new Paper();
        HttpGet httpGet = new HttpGet(link);
        httpGet.setHeader("Referer", CNKIConsts.REFERER);
        String html = null;
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(httpGet);
        ) {
            HttpEntity entity = response.getEntity();
            html = EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document doc = Jsoup.parse(html);
        //for gc
        html = null;
        Optional<Elements> orgns = Optional.ofNullable(doc.getElementsByClass("orgn"));
        orgns.map(Elements::text).ifPresent(paper::setOrganization);
        Optional<Element> chDivSummary = Optional.ofNullable(doc.getElementById("ChDivSummary"));
        chDivSummary.map(Element::text).ifPresent(paper::setSummary);
        Optional<Element> keyword = Optional.ofNullable(doc.getElementById("catalog_KEYWORD"));
        keyword.map(Element::nextElementSiblings).ifPresent(elements -> {
            Optional<String> res = elements.stream().map(Element::text).reduce((x, y) -> x + y);
            paper.setKeyword(res.get());
        });
        //中文分类号
        Optional<Element> ztcls = Optional.ofNullable(doc.getElementById("catalog_ZTCLS"));
        ztcls.map(Element::parent).map(Element::ownText).ifPresent(paper::setZtcls);
        List<Binding<FileType, String>> downloadInfo = new ArrayList<>();
        Optional<Element> cajDown = Optional.ofNullable(doc.getElementById("cajDown"));
        cajDown.ifPresent(e -> {
            String cajDownloadLink = e.attr("href");
            if (!cajDownloadLink.contains(CNKIConsts.CNKI_HOME)) {
                cajDownloadLink = CNKIConsts.CNKI_HOME + cajDownloadLink;
            }
            Binding<FileType, String> cajBinding = new Binding<>(FileType.CAJ, cajDownloadLink.trim());
            downloadInfo.add(cajBinding);
        });
        Optional<Element> pdfDown = Optional.ofNullable(doc.getElementById("pdfDown"));
        pdfDown.ifPresent(e -> {
            String pdfDownloadLink = e.attr("href");
            if (!pdfDownloadLink.contains(CNKIConsts.CNKI_HOME)) {
                pdfDownloadLink = CNKIConsts.CNKI_HOME + pdfDownloadLink;
            }
            Binding<FileType, String> pdfBinding = new Binding<>(FileType.PDF, pdfDownloadLink.trim());
            downloadInfo.add(pdfBinding);
        });
        paper.setDownloadInfo(downloadInfo);
        return paper;
    }

    /**
     * 根据下载链接得到响应
     *
     * @param downloadLink
     */
    @Override
    public InputStream getDownloadResource(String downloadLink) {
        try {
            downloadLink = URLDecoder.decode(downloadLink, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("解析下载链接失败");
            e.printStackTrace();
        }
        RequestConfig requestConfig;
        if (downloadLink.contains(CNKIConsts.CNKI_HOST)) {
            requestConfig = RequestConfig.DEFAULT;
        } else {
            requestConfig = RequestConfig
                    .custom()
                    .setProxy(new HttpHost(CNKIConsts.CNKI_HOST))
                    .setRedirectsEnabled(true)
                    .build();
        }
        HttpGet httpGet = new HttpGet(downloadLink);
        httpGet.setConfig(requestConfig);
        ResponseHandler<InputStream> responseHandler = httpResponse -> {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            //have enabled redirect
            if (statusCode >= 300) {
                log.error("请求下载失败，响应状态码为{}", statusCode);
                throw new ClientProtocolException("Unexpected response status: " + statusCode);
            }
            HttpEntity entity = httpResponse.getEntity();
            if (entity.isStreaming()) {
                return entity.getContent();
            }
            return null;
        };
        InputStream inputStream = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            inputStream = client.execute(httpGet, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("获取响应资源成功");
        return inputStream;
    }

    /**
     * 知网高级检索
     *
     * @param condition
     * @return
     */
    @Override
    public SearchResult advancedSearch(AdvancedSearchCondition condition) {
        ChromeDriver driver = new ChromeDriver();
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.pageLoadTimeout(10, TimeUnit.SECONDS);
        timeouts.implicitlyWait(100, TimeUnit.MILLISECONDS);
        driver.get(CNKIConsts.ADVANCED_SEARCH_HOME);
        SearchResult searchResult = null;
        try {
            commonService.handleConditionAndSearch(condition, driver);
            searchResult = commonService.extractSearchResult(driver);
        } catch (Exception e) {
            log.error("解析高级检索结果失败");
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return searchResult;
    }

    /**
     * 根据链接和账户id获取下一页的查询内容
     *
     * @param pageLink
     * @param accountId
     */
    @Override
    public SearchResult getEntriesByPageLink(String pageLink, int accountId) {
        try {
            pageLink = URLDecoder.decode(pageLink, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        CookieStore cookieStore = cookieCache.get(accountId);
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        HttpGet httpGet = new HttpGet(pageLink);
        httpGet.setHeader("Referer", referLink.getOrDefault(accountId, CNKIConsts.REFERER));
        if (referLink.size() > 256) {
            referLink.clear();
        }
        referLink.put(accountId, pageLink);
        String resultHtml = null;
        if (client == null) {
            client = HttpClients.custom()
                    .setDefaultCookieStore(cookieStore)
                    .setDefaultRequestConfig(globalConfig)
                    .setProxy(new HttpHost(CNKIConsts.CNKI_HOST))
                    .setMaxConnTotal(1000)
                    .build();
        }
        try (CloseableHttpResponse response = client.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            resultHtml = EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (resultHtml.contains("验证码")) {
            throw new SearchingException(SearchError.REQUEST_TOO_MUCH);
        }
        SearchResult searchResult;
        try {
            searchResult = parsingService.parseHtml(resultHtml);
        } catch (SearchingException e) {
            try {
                client.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                client = null;
            }
            throw e;
        } finally {
            //for gc
            resultHtml = null;
        }
        return searchResult;
    }
}
