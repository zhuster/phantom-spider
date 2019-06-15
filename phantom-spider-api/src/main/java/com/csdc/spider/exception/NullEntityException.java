package com.csdc.spider.exception;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NullEntityException extends RuntimeException {

    private static final long serialVersionUID = 2064858752580795109L;
    private Class<?> nullClass;
    private String nullObjectName;

    public NullEntityException(Class<?> nullClass, String nullObjectName) {
        this.nullClass = nullClass;
        this.nullObjectName = nullObjectName;
    }

}
