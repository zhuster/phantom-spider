package com.csdc.spider.enumeration;

/**
 * 高级检索条件之间的逻辑运算符
 *
 * @author zhangzhi
 * @since <pre>2019/6/7</pre>
 */
public enum LogicalOperatorEnum {
    AND("#CNKI_AND"), OR("#CNKI_OR"), NOT("#CNKI_NOT");

    private final String code;

    LogicalOperatorEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
