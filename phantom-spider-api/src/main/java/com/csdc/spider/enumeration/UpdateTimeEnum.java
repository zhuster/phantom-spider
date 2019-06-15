package com.csdc.spider.enumeration;

/**
 * 高级检索更新时间
 *
 * @author zhangzhi
 * @since <pre>2019/6/8</pre>
 */
public enum UpdateTimeEnum {
    DEFAULT("不限"),
    RECENT_WEEK("最近一周"),
    RECENT_MONTH("最近一月"),
    RECENT_HALF_YEAR("最近半年"),
    RECENT_YEAR("最近一年"),
    SO_FAR_THIS_YEAR("今年迄今"),
    LAST_YEAR("上一年度");

    private final String code;

    UpdateTimeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
