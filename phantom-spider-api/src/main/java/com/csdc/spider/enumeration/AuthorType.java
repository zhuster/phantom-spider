package com.csdc.spider.enumeration;

/**
 * 高级检索作者类型
 *
 * @author zhangzhi
 * @since <pre>2019/6/8</pre>
 */
public enum AuthorType {
    AUTHOR("AU"), FIRST_AUTHOR("FI"), CORRESPONDING_AUTHOR("RP");

    private final String code;

    AuthorType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
