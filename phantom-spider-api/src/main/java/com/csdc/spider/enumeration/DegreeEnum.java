package com.csdc.spider.enumeration;

/**
 * 高级条件检索的查询程度：精确or模糊
 *
 * @author zhangzhi
 * @since <pre>2019/6/8</pre>
 */
public enum DegreeEnum {
    EXACT("精确"), FUZZY("模糊");

    private final String code;

    DegreeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
