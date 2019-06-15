package com.csdc.spider.enumeration;

/**
 * 检索内容所属种类
 *
 * @author zhangzhi
 * @since <pre>2019/5/29</pre>
 */
public enum SearchContentType {
    SUBJECT("主题"),
    KEYWORD("关键词"),
    TITLE("篇名"),
    FULL_TEXT("全文"),
    AUTHOR("作者"),
    COMPANY("单位"),
    ABSTRACT("摘要"),
    REFERENCES("被引文献"),
    CLCN("中图分类号"),//chinese library classification number --> 中图分类号
    LITERATURE_RESOURECES("文献来源");


    private final String type;

    SearchContentType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
