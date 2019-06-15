package com.csdc.spider.enumeration;

/**
 * 高级检索的搜索词语的扩展类型：中英文扩展or同义词扩展
 *
 * @author zhangzhi
 * @since <pre>2019/6/8</pre>
 */
public enum WordExtensionType {
    ZH_EN("txt_extensionCKB"), SYNONYM("txt_extensionCKB_R");

    private final String code;

    WordExtensionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
