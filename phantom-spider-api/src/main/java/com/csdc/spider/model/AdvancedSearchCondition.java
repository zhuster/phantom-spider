package com.csdc.spider.model;

import com.csdc.spider.enumeration.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 高级检索条件
 * click link below for more details
 * <a>http://kns.cnki.net/kns/brief/result.aspx?dbprefix=SCDB&crossDbcodes=CJFQ,CDFD,CMFD,CPFD,IPFD,CCND,CCJD</a>
 *
 * @author zhangzhi
 * @since <pre>2019/6/8</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedSearchCondition implements Serializable {

    private static final long serialVersionUID = -840662859336349225L;

    private SearchContentType contentType;
    private String searchValue1;
    private Integer wordFreq1;
    private LogicalOperatorEnum searchLogical;
    private String searchValue2;
    private Integer wordFreq2;
    private DegreeEnum searchContentDegree;
    private AuthorType authorType;
    private String authorName;
    private DegreeEnum searchAuthorDegree;
    private String authorCompany;
    private DegreeEnum searchAuthorCompanyDegree;
    private String publishDateFrom;
    private String publishDateTo;
    private UpdateTimeEnum updateTime;
    private String source;
    private DegreeEnum searchSourceDegree;
    private String fund;
    private DegreeEnum searchFundDegree;
    private boolean firstPubOnline;
    private boolean enhancePub;
    private boolean dataPaper;
    private WordExtensionType wordExtensionType;


}
