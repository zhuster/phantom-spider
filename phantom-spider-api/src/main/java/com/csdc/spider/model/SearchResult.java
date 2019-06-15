package com.csdc.spider.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
/**
 * @author zhangzhi
 * @since <pre>2019/6/12</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult implements Serializable {
    private static final long serialVersionUID = -7527523190111970530L;

    private List<Entry> entries;
    private Integer total;
    private String nextPageLink;
    private String prevPageLink;

}
