package com.github.yungyu16.intellij.sqllogparser.error;

/**
 * CreatedDate: 2020/11/5
 * Author: songjialin
 */
public class TipException extends RuntimeException {
    public TipException() {
    }

    public TipException(String message) {
        super(message);
    }

    public TipException(String message, Throwable cause) {
        super(message, cause);
    }

    public TipException(Throwable cause) {
        super(cause);
    }

    public TipException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
