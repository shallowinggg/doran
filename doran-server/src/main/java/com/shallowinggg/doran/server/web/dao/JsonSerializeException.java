package com.shallowinggg.doran.server.web.dao;

import com.shallowinggg.doran.common.util.ClassUtils;

/**
 * @author shallowinggg
 */
public class JsonSerializeException extends Exception {

    public JsonSerializeException(Throwable cause) {
        super(cause);
    }

    public JsonSerializeException(Object val, Throwable cause) {
        super("Fail to serialize object with type " + ClassUtils.getDescriptiveType(val), cause);
    }

}
