package com.csdc.spider.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author zhangzhi
 * @since <pre>2019/5/31</pre>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Entry implements Serializable {

    private static final long serialVersionUID = -1615987685936353437L;

    private Integer id;
    private Binding<String, String> paperBinding;
    private String authorNames;
    private String source;
    private String postDate;
    private String dateBase;
    private Integer citedTimes;
    private String downloadLink;


}
