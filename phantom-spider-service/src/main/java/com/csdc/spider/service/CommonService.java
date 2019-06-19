package com.csdc.spider.service;

import com.csdc.spider.enumeration.*;
import com.csdc.spider.exception.NullEntityException;
import com.csdc.spider.model.*;
import com.csdc.spider.util.CNKIConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangzhi
 * @since <pre>2019/6/2</pre>
 */
@Slf4j
@Service
public class CommonService {

    @Autowired
    ParsingService parsingService;

    /**
     * 提取文章条目信息
     *
     * @param driver
     */
    public SearchResult extractSearchResult(ChromeDriver driver) throws InterruptedException {
        driver.switchTo().frame("iframeResult");
        // 50 entries each page
        new WebDriverWait(driver, 2).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"id_grid_display_num\"]/a[3]")));
        WebElement fiftyElement = driver.findElementByXPath("//*[@id=\"id_grid_display_num\"]/a[3]");
        driver.executeScript("arguments[0].click()", fiftyElement);
        String pageSource = driver.getPageSource();
//      下面代码用selenium提取页面信息效率很低
/*        String currentUrl = driver.getCurrentUrl();
        System.out.println("current-url--->"+currentUrl);

        WebElement paperTitleCell = driver.findElementByXPath("//*[@id=\"ctl00\"]/table/tbody/tr[3]/td/table/tbody/tr/td/div/div");
        String rawNum = paperTitleCell.getText();
        SearchResult searchResult = new SearchResult();
        int totalNum = parsingService.specifyTotalNum(rawNum);
        searchResult.setTotal(totalNum);
        if (totalNum > 50) {
            String nextPageLink = findNextPageLink(driver);
            searchResult.setNextPageLink(nextPageLink);
        }
        List<Entry> entries = new ArrayList<>();
        WebElement body = driver.findElement(By.xpath("//*[@id=\"ctl00\"]/table/tbody/tr[2]/td/table/tbody"));
        List<WebElement> trs = body.findElements(By.tagName("tr"));
        trs.remove(0);
        trs.forEach(tr -> {
            List<WebElement> tds = tr.findElements(By.tagName("td"));
            Entry entry;
            try {
                entry = extractPaperEntry(tds, driver);
            } catch (Exception e) {
                log.error("提取文章信息失败");
                throw e;
            }
            entries.add(entry);
        });
        searchResult.setEntries(entries);*/
        SearchResult searchResult = parsingService.parseHtml(pageSource);
        return searchResult;
    }


    /**
     * 查找到下一页的链接
     *
     * @param driver
     */
    private String findNextPageLink(ChromeDriver driver) {
        WebElement next = driver.findElementById("Page_next");
        String link = next.getAttribute("href");
        return link;
    }


    /**
     * 找到检索首页对应的条目信息
     *
     * @param tds td标签列表每一个td里面都包含着paper信息
     */
    public Entry extractPaperEntry(List<WebElement> tds, WebDriver driver) {
        Entry entry = new Entry();
        for (int i = 0; i < 8; i++) {
            switch (i) {
                case 0:
                    Integer id = Integer.parseInt(tds.get(i).getText());
                    entry.setId(Integer.valueOf(id));
                    break;
                case 1:
                    WebElement td = tds.get(i);
                    String name = td.getText();
                    WebElement a = td.findElement(By.className("fz14"));
                    String link = a.getAttribute("href");
                    Binding<String, String> paperBinding = new Binding<>(name, link);
                    entry.setPaperBinding(paperBinding);
                    break;
                case 2:
                    String authorNames = tds.get(i).getText();
                    entry.setAuthorNames(authorNames);
                    break;
                case 3:
                    String source = tds.get(i).getText();
                    entry.setSource(source);
                    break;
                case 4:
                    String postDate = tds.get(i).getText();
                    entry.setPostDate(postDate);
                    break;
                case 5:
                    String dateBase = tds.get(i).getText();
                    entry.setDateBase(dateBase);
                    break;
                case 6:
                    String rawCitedTimes = tds.get(i).getText();
                    if (StringUtils.isEmpty(rawCitedTimes)) {
                        entry.setCitedTimes(0);
                        break;
                    }
                    Integer citedTimes = Integer.parseInt(rawCitedTimes);
                    entry.setCitedTimes(citedTimes);
                    break;
                case 7:
                    try {
                        WebElement downloadTd = tds.get(i);
                        new WebDriverWait(driver, 0L).until(ExpectedConditions.presenceOfNestedElementLocatedBy(downloadTd, By.tagName("a")));
                        WebElement downloadA = downloadTd.findElement(By.tagName("a"));
                        if (downloadA.getAttribute("class").equals("briefDl_Y")) {
                            String downloadLink = downloadA.getAttribute("href");
                            entry.setDownloadLink(downloadLink);
                        }
                    } catch (Exception e) {
                        log.debug("未找到文章{}下载信息", entry.getId());
                    }
                    break;
                default:
                    break;
            }
        }
        return entry;
    }

    /**
     * 用driver触发的方式提取文章的详情
     *
     * @param driver
     * @param element replaced by method below
     * @see #handleConditionAndSearch(AdvancedSearchCondition, ChromeDriver)
     */
    @Deprecated
    private Paper extractPaperInfo(WebDriver driver, WebElement element) {
        element.click();
        String currentWindow = driver.getWindowHandle();
        Set<String> windowHandles = driver.getWindowHandles();
        Optional<String> other = windowHandles.stream().filter(e -> !currentWindow.equals(e)).findFirst();
        other.orElseThrow(() -> new NullEntityException(String.class, "windowHandle"));
        driver.switchTo().window(other.get());
        Paper paper = new Paper();
        String title = driver.findElement(By.className("title")).getText();
        paper.setTitle(title);
        WebElement authorElement = driver.findElement(By.className("author"));
        List<WebElement> authorAs = authorElement.findElements(By.tagName("a"));
        Set<String> authors = new HashSet<>();
        authorAs.parallelStream().map(WebElement::getText).forEach(authors::add);
        paper.setAuthors(authors);
        //organization
        String orgn = driver.findElement(By.className("orgn")).getText();
        paper.setOrganization(orgn);
        //summary
        String summary = driver.findElement(By.id("catalog_ABSTRACT")).getText();
        paper.setSummary(summary);
        //fund
        try {
            String fund = driver.findElement(By.id("catalog_FUND")).getText();
            paper.setFund(fund);
        } catch (Exception e) {

        }
        //keyword
        String keyword = driver.findElement(By.id("catalog_KEYWORD")).getText();
        paper.setKeyword(keyword);
        //downloadLinks
        List<Binding<FileType, String>> downloadInfo = new ArrayList<>();
        try {
            WebElement cajElement = driver.findElement(By.id("cajDown"));
            String downloadLink = cajElement.getAttribute("href");
            Binding<FileType, String> cajBinding = new Binding<>(FileType.CAJ, downloadLink);
            downloadInfo.add(cajBinding);
        } catch (Exception e) {
            log.warn("未找到{}的caj下载元素", title);
            e.printStackTrace();
        }
        try {
            WebElement pdfElement = driver.findElement(By.id("pdfDown"));
            String downloadLink = pdfElement.getAttribute("href");
            Binding<FileType, String> pdfBinding = new Binding<>(FileType.PDF, downloadLink);
            downloadInfo.add(pdfBinding);
        } catch (Exception e) {
            log.warn("未找到{}的pdf下载元素", title);
            e.printStackTrace();
        }
        paper.setDownloadInfo(downloadInfo);
        driver.close();
        driver.switchTo().window(currentWindow);
        return paper;
    }

    /**
     * 处理高级检索的条件并驱动检索
     *
     * @param condition {@link AdvancedSearchCondition}
     * @param driver    浏览器驱动保证单线程内唯一性
     */
    public void handleConditionAndSearch(AdvancedSearchCondition condition, final ChromeDriver driver) {
        Optional<SearchContentType> contentType = Optional.ofNullable(condition.getContentType());
        contentType.ifPresent(e -> {
            String content = e.getType();
            WebElement sel = driver.findElementById("txt_1_sel");
            Select select = new Select(sel);
            select.selectByVisibleText(content);
        });
        Optional<String> searchValue1 = Optional.ofNullable(condition.getSearchValue1());
        searchValue1.ifPresent(driver.findElementById("txt_1_value1")::sendKeys);
        Optional<Integer> wordFreq1 = Optional.ofNullable(condition.getWordFreq1());
        wordFreq1.ifPresent(e -> {
            WebElement freq = driver.findElementById("txt_1_freq1");
            Select select = new Select(freq);
            select.selectByValue(e.toString());
        });
        Optional<LogicalOperatorEnum> searchLogical = Optional.ofNullable(condition.getSearchLogical());
        searchLogical.ifPresent(e -> {
            String logicalOperator = e.getCode();
            WebElement relation = driver.findElementById("txt_1_relation");
            Select select = new Select(relation);
            select.selectByValue(logicalOperator);
        });
        Optional<String> searchValue2 = Optional.ofNullable(condition.getSearchValue2());
        searchValue2.ifPresent(driver.findElementById("txt_1_value2")::sendKeys);
        Optional<Integer> wordFreq2 = Optional.ofNullable(condition.getWordFreq2());
        wordFreq2.ifPresent(e -> {
            WebElement freq = driver.findElementById("txt_1_freq2");
            Select select = new Select(freq);
            select.selectByValue(e.toString());
        });
        Optional<DegreeEnum> searchContentDegree = Optional.ofNullable(condition.getSearchContentDegree());
        searchContentDegree.ifPresent(e -> {
            WebElement special1 = driver.findElementById("txt_1_special1");
            Select select = new Select(special1);
            String code = e.getCode();
            select.selectByVisibleText(code);
        });
        Optional<AuthorType> authorType = Optional.ofNullable(condition.getAuthorType());
        authorType.ifPresent(e -> {
            WebElement auSel = driver.findElementById("au_1_sel");
            Select select = new Select(auSel);
            String code = e.getCode();
            select.selectByValue(code);
        });
        Optional<String> authorName = Optional.ofNullable(condition.getAuthorName());
        authorName.ifPresent(driver.findElementById("au_1_value1")::sendKeys);
        Optional<DegreeEnum> searchAuthorDegree = Optional.ofNullable(condition.getSearchAuthorDegree());
        searchAuthorDegree.ifPresent(e -> {
            String code = e.getCode();
            WebElement special1 = driver.findElementById("au_1_special1");
            Select select = new Select(special1);
            select.selectByVisibleText(code);
        });
        Optional<String> authorCompany = Optional.ofNullable(condition.getAuthorCompany());
        authorCompany.ifPresent(driver.findElementById("au_1_value2")::sendKeys);
        Optional<DegreeEnum> searchAuthorCompanyDegree = Optional.ofNullable(condition.getSearchAuthorCompanyDegree());
        searchAuthorCompanyDegree.ifPresent(e -> {
            WebElement special2 = driver.findElementById("au_1_special2");
            Select select = new Select(special2);
            String code = e.getCode();
            select.selectByVisibleText(code);
        });
        Optional<String> publishDateFrom = Optional.ofNullable(condition.getPublishDateFrom());
        publishDateFrom.ifPresent(e -> driver.findElementById("publishdate_from").sendKeys(e));
        Optional<String> publishDateTo = Optional.ofNullable(condition.getPublishDateTo());
        publishDateTo.ifPresent(e -> driver.findElementById("publishdate_to").sendKeys(e));
        Optional<UpdateTimeEnum> updateTime = Optional.ofNullable(condition.getUpdateTime());
        updateTime.ifPresent(e -> {
            String code = e.getCode();
            WebElement dateOpt = driver.findElementById("updatedateN_opt");
            Select select = new Select(dateOpt);
            select.selectByVisibleText(code);
        });
        Optional<String> source = Optional.ofNullable(condition.getSource());
        source.ifPresent(driver.findElementById("magazine_value1")::sendKeys);
        Optional<DegreeEnum> searchSourceDegree = Optional.ofNullable(condition.getSearchSourceDegree());
        searchSourceDegree.ifPresent(e -> {
            WebElement magazineSpecial1 = driver.findElementById("magazine_special1");
            Select select = new Select(magazineSpecial1);
            String code = e.getCode();
            select.selectByVisibleText(code);
        });
        Optional<String> fund = Optional.ofNullable(condition.getFund());
        fund.ifPresent(driver.findElementById("base_value1")::sendKeys);
        Optional<DegreeEnum> searchFundDegree = Optional.ofNullable(condition.getSearchFundDegree());
        searchFundDegree.ifPresent(e -> {
            WebElement baseSpecial1 = driver.findElementById("base_special1");
            String code = e.getCode();
            Select select = new Select(baseSpecial1);
            select.selectByVisibleText(code);
        });
        if (condition.isFirstPubOnline()) {
            WebElement pubOnline = driver.findElementById("checkType1");
            driver.executeScript("arguments[0].click()", pubOnline);
        }
        if (condition.isEnhancePub()) {
            WebElement enhancePub = driver.findElementById("checkType2");
            driver.executeScript("arguments[0].click()", enhancePub);
        }
        if (condition.isDataPaper()) {
            WebElement dataPaper = driver.findElementById("checkType3");
            driver.executeScript("arguments[0].click()", dataPaper);
        }
        Optional<WordExtensionType> wordExtensionType = Optional.ofNullable(condition.getWordExtensionType());
        wordExtensionType.map(WordExtensionType::getCode)
                .map(driver::findElementById)
                .ifPresent(WebElement::click);
        //trigger search
        WebElement btnSearch = driver.findElementById("btnSearch");
        driver.executeScript("arguments[0].click()", btnSearch);
    }

    /**
     * 将WebDriver中的cookie转换为HttpClient中的cookie并添加进CookieStore
     *
     * @param driver
     */
    public CookieStore getCookieStore(WebDriver driver) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        Set<Cookie> cookies = driver.manage().getCookies();
        cookies.forEach(e -> {
            BasicClientCookie basicClientCookie = new BasicClientCookie(e.getName(), e.getValue());
            basicClientCookie.setPath(e.getPath());
            basicClientCookie.setDomain(e.getDomain());
            basicClientCookie.setExpiryDate(e.getExpiry());
            basicClientCookie.setSecure(e.isSecure());
            cookieStore.addCookie(basicClientCookie);
        });
        return cookieStore;
    }

    /**
     * Jsoup提取响应得到的html内容
     *
     * @param elements
     * @return
     */
    public List<Entry> extractPaperEntry(Elements elements) {
        List<Entry> entries = new ArrayList<>();
        elements.forEach(e -> {
            Elements children = e.children();
            Entry entry = new Entry();
            for (int i = 0; i < 8; i++) {
                switch (i) {
                    case 0:
                        String rawId = children.get(i).text();
                        int id = Integer.parseInt(rawId);
                        entry.setId(id);
                        break;
                    case 1:
                        Element td = children.get(i);
                        Element a = td.child(0);
                        String title = a.text();
                        String link = a.attr("href");
                        link = CNKIConsts.CNKI_HOME + link;
                        Binding<String, String> paperInfo = new Binding<>(title, link);
                        entry.setPaperBinding(paperInfo);
                        break;
                    case 2:
                        String authors = children.get(i).text();
                        entry.setAuthorNames(authors);
                        break;
                    case 3:
                        Element sourceElement = children.get(i);
                        String source = sourceElement.text();
                        entry.setSource(source);
                        break;
                    case 4:
                        String postDate = children.get(i).text();
                        entry.setPostDate(postDate);
                        break;
                    case 5:
                        String dateBase = children.get(i).text();
                        entry.setDateBase(dateBase);
                        break;
                    case 6:
                        String rawCitedTimes = children.get(i).text();
                        if (StringUtils.isEmpty(rawCitedTimes)) {
                            entry.setCitedTimes(0);
                            break;
                        }
                        Integer citedTimes = Integer.parseInt(rawCitedTimes);
                        entry.setCitedTimes(citedTimes);
                        break;
                    case 7:
                        Element downloadElement = children.get(i);
                        if (downloadElement.children().isEmpty()) {
                            log.debug("文章{}缺失下载信息", entry.getId());
                            break;
                        }
                        Element child = downloadElement.child(0);
                        String downloadLink = child.attr("href");
                        downloadLink = downloadLink.replace("..", CNKIConsts.CNKI_DOWNLOAD_HOME);
                        entry.setDownloadLink(downloadLink);
                        break;
                    default:
                        break;
                }
            }
            entries.add(entry);
        });
        return entries;
    }

    /**
     * 根据用户ID存储或者更新响应的cookie当容量超过256时清空cookie
     *
     * @param driver
     * @param cookieCache
     * @param accountId
     */
    public void handleCookie(WebDriver driver, ConcurrentHashMap<Integer, CookieStore> cookieCache, int accountId) {
        if (cookieCache.size() > 256) {
            cookieCache.clear();
        }
        CookieStore cookieStore = getCookieStore(driver);
        cookieCache.put(accountId, cookieStore);
    }
}
