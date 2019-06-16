package com.csdc.spider.dubbo.cnki;


import com.csdc.spider.api.SearchService;
import com.csdc.spider.enumeration.FileType;
import com.csdc.spider.enumeration.SearchContentType;
import com.csdc.spider.enumeration.error.SearchError;
import com.csdc.spider.exception.SearchingException;
import com.csdc.spider.model.*;
import com.csdc.spider.service.CommonService;
import com.csdc.spider.util.CNKIConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.csdc.spider.util.ConfigConsts.TMP_HTML_LOCATION;

/**
 * 知网检索与条件检索服务端实现
 * <note>在消费端调用服务端接口方法的时候入参必须进行充分验证，服务端不再进行验证</note>
 *
 * @author zhangzhi
 * @since <pre>2019/5/29</pre>
 */
@Slf4j
@org.apache.dubbo.config.annotation.Service(group = "cnki", version = "1.0.1")
@Service
public class SearchServiceImpl implements SearchService {


    private static Lock lock = new ReentrantLock();
    private static Lock pageLock = new ReentrantLock();
    private static final ThreadLocal<CookieStore> COOKIE_STORE_THREAD_LOCAL = new ThreadLocal<>();
    private static AtomicReference<CookieStore> simpleCookieStore = new AtomicReference<>();


    @Autowired
    CommonService commonService;

    static {
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver\\chromedriver.exe");
    }


    /**
     * 调用知网的初级检索，根据检索内容与内容种类驱动检索并提取检索内容
     * Note:注意异常调用<code>quit</code>方法，遇到过虚拟机(mem:2G)上oom的情况
     *
     * @param searchContentType 输入内容类型
     * @param content           输入内容
     */
    @Override
    public SearchResult simpleSearch(SearchContentType searchContentType, String content) {
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
            CookieStore cookieStore = commonService.getCookieStore(driver);
            CookieStore oldCookieStore = simpleCookieStore.get();
            simpleCookieStore.compareAndSet(oldCookieStore, cookieStore);
            driver.quit();
        }
        if (Objects.isNull(searchContentType)) {
            log.info("查询类型：主题 查询内容：{}", content);
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
        lock.lock();
        File directory = new File(TMP_HTML_LOCATION);
        if (!directory.exists()) {
            directory.mkdir();
        }
        Path path = null;
        try {
            path = Files.createTempFile(directory.toPath(), "buffer", "html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(httpGet);
             FileOutputStream fos = new FileOutputStream(path.toFile())
        ) {
            response.getEntity().writeTo(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document doc = null;
        try {
            doc = Jsoup.parse(path.toFile(), "UTF-8");
            Files.delete(path);
        } catch (IOException e) {
            log.error("解析html失败");
            e.printStackTrace();
        }
        lock.unlock();
        Optional<Elements> orgns = Optional.ofNullable(doc.getElementsByClass("orgn"));
        orgns.map(Elements::text).ifPresent(paper::setOrganization);
        Optional<Element> chDivSummary = Optional.ofNullable(doc.getElementById("ChDivSummary"));
        chDivSummary.map(Element::text).ifPresent(paper::setSummary);
        Optional<Element> keyword = Optional.ofNullable(doc.getElementById("catalog_KEYWORD"));
        keyword.map(Element::nextElementSiblings).ifPresent(elements->{
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
            Binding<FileType, String> cajBinding = new Binding<>(FileType.CAJ, cajDownloadLink);
            downloadInfo.add(cajBinding);
        });
        Optional<Element> pdfDown = Optional.ofNullable(doc.getElementById("pdfDown"));
        pdfDown.ifPresent(e -> {
            String pdfDownloadLink = e.attr("href");
            Binding<FileType, String> pdfBinding = new Binding<>(FileType.PDF, pdfDownloadLink);
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
    public HttpResponse getDownloadResponse(String downloadLink) {
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
        ResponseHandler<HttpResponse> responseHandler = httpResponse -> {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            //have enabled redirect
            if (statusCode >= 300) {
                log.error("请求下载失败，响应状态码为{}", statusCode);
                throw new ClientProtocolException("Unexpected response status: " + statusCode);
            }
            return httpResponse;
        };
        HttpResponse response = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            response = client.execute(httpGet, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("获取响应资源成功");
        return response;
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

    @Override
    public SearchResult getEntriesByPageLink(String pageLink) {
        try {
            pageLink = URLDecoder.decode(pageLink, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        CookieStore cookieStore = simpleCookieStore.get();
        RequestConfig requestConfig = RequestConfig.custom()
                .setProxy(new HttpHost(CNKIConsts.CNKI_HOST))
                .build();
        HttpGet httpGet = new HttpGet(pageLink);
        httpGet.setHeader("Referer", CNKIConsts.REFERER);
        String resultHtml = null;
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = client.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            resultHtml = EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document doc = Jsoup.parse(resultHtml);
        Elements trs;
        try {
            Elements content = doc.getElementsByClass("GridTableContent");
            Element table = content.get(0);
            Element child = table.child(0);
            trs = child.getElementsByAttribute("bgcolor");
        } catch (Exception e) {
            throw new SearchingException(SearchError.COOKIE_HAS_EXPIRED);
        }
        SearchResult searchResult = new SearchResult();
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
            if (!link.contains(CNKIConsts.REFERER)) {
                link = CNKIConsts.REFERER + link;
            }
            searchResult.setPrevPageLink(link);
        });
        Optional<Element> next = Optional.ofNullable(doc.getElementById("Page_next"));
        next.ifPresent(e -> {
            String link = e.attr("href");
            if (!link.contains(CNKIConsts.REFERER)) {
                link = CNKIConsts.REFERER + link;
            }
            searchResult.setNextPageLink(link);
        });
        return searchResult;
    }
}
