package com.csdc.spider;

import com.csdc.spider.dubbo.cnki.SearchServiceImpl;
import com.csdc.spider.enumeration.*;
import com.csdc.spider.model.AdvancedSearchCondition;
import com.csdc.spider.model.Entry;
import com.csdc.spider.model.Paper;
import com.csdc.spider.model.SearchResult;
import org.apache.dubbo.common.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PhantomSpiderServiceApplicationTests {

    @Autowired
    SearchServiceImpl searchService;

    @Test
    public void contextLoads() {
        AdvancedSearchCondition condition = new AdvancedSearchCondition();
        condition.setContentType(SearchContentType.KEYWORD);
//        condition.setSearchValue1("社科");
        condition.setWordFreq1(2);
        condition.setSearchLogical(LogicalOperatorEnum.OR);
//        condition.setSearchValue2("深度学习");
        condition.setSearchContentDegree(DegreeEnum.FUZZY);
        condition.setAuthorType(AuthorType.CORRESPONDING_AUTHOR);
//        condition.setAuthorName("王玉明");
        condition.setSearchAuthorDegree(DegreeEnum.FUZZY);
        condition.setAuthorCompany("华中科技大学");
        condition.setSearchAuthorCompanyDegree(DegreeEnum.EXACT);
        LocalDate from = LocalDate.of(2010, 3, 28);
        LocalDate to = LocalDate.of(2018, 9, 9);
        condition.setPublishDateFrom(from);
        condition.setPublishDateTo(to);
        condition.setUpdateTime(UpdateTimeEnum.LAST_YEAR);
        System.out.println();
//        searchService.advancedSearch(condition).forEach(System.out::println);
    }

    @Test
    public void TestAdvSearch(){
        AdvancedSearchCondition condition = new AdvancedSearchCondition();
        condition.setContentType(SearchContentType.KEYWORD);
//        condition.setSearchValue1("社科");
        condition.setWordFreq1(2);
        condition.setSearchLogical(LogicalOperatorEnum.OR);
//        condition.setSearchValue2("深度学习");
        condition.setSearchContentDegree(DegreeEnum.FUZZY);
        condition.setAuthorType(AuthorType.CORRESPONDING_AUTHOR);
//        condition.setAuthorName("王玉明");
        condition.setSearchAuthorDegree(DegreeEnum.FUZZY);
        condition.setAuthorCompany("华中科技大学");
        condition.setSearchAuthorCompanyDegree(DegreeEnum.EXACT);
        LocalDate from = LocalDate.of(2010, 3, 28);
        LocalDate to = LocalDate.of(2018, 9, 9);
        condition.setPublishDateFrom(from);
        condition.setPublishDateTo(to);
        condition.setUpdateTime(UpdateTimeEnum.LAST_YEAR);
        condition.setSource("期刊");
        condition.setSearchFundDegree(DegreeEnum.FUZZY);
        condition.setFund("基金");
        condition.setSearchFundDegree(DegreeEnum.FUZZY);
        condition.setFirstPubOnline(true);
        condition.setEnhancePub(true);
        condition.setDataPaper(true);
        condition.setWordExtensionType(WordExtensionType.SYNONYM);
//        searchService.advancedSearch(condition).forEach(System.out::println);

    }

    @Test
    public void simpleSearchTest(){
        SearchResult searchResult = searchService.simpleSearch(null, "机器学习");
        System.out.println(searchResult.getTotal());
        searchResult.getEntries().forEach(System.out::println);
        System.out.println(searchResult.getNextPageLink());
    }


    @Test
    public void findPaperInfoTest(){
        Paper paperInfo = searchService.findPaperInfo("http://kns.cnki.net/kns/detail/detail.aspx?QueryID=0&CurRec=133&recid=&FileName=XTBR201906050040&DbName=CCNDPREP&DbCode=CCND&yx=&pr=&URLID=&bsm=");
        System.out.println(paperInfo);
    }

    @Test
    public void getEntriesByPageLinkTest(){
        String link = "";
        SearchResult result = searchService.getEntriesByPageLink(link);
        result.getEntries().forEach(System.out::println);
    }

    @Test
    public void downloadTest(){
        String url = "http://kns.cnki.net/kns/download.aspx?filename=Fc3UjVZRDOsBzR6BjazoXcSVGN6hFMVlWSjJUNoFVVwk2SiV3UIR1Z5gnbxo2d2d3K6FlNsR0cwI0dStGVmZlR=0TPBRXQo5kahplVP9GWxgFWkFnWYhnYChjdrZWQ05mSqplYjBnWTJlbt1UbnF3ToZmNSlUNnFFTZhHWyBVUyA&tablename=CCNDCOMMIT_DAY";
        url = URL.encode(url);
        searchService.getDownloadResponseEntity(url);
    }

}
