package com.csdc.spider.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @param <T> 目标对象
 * @param <D> 绑定的属性
 * @author zhangzhi
 * @since <pre>2019/6/2</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Binding<T, D> implements Serializable {

    private static final long serialVersionUID = -8984018095008128236L;

    private T obj;
    private D attr;
}
