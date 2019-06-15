package com.csdc.spider.model;

import com.csdc.spider.enumeration.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author zhangzhi
 * @since <pre>2019/6/4</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paper implements Serializable {

    private static final long serialVersionUID = 4697087484737524007L;

    private String title;
    private Set<String> authors;
    private String organization;
    private String summary;
    private String fund;
    private String keyword;
    private String ztcls;//中文分类号
    private List<Binding<FileType, String>> downloadInfo;

}
