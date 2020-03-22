package com.shallowinggg.doran.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.RequestMQConfigResponseHeader;
import org.junit.Test;

public class PojoHeaderConverterTest {

    @Test
    public void test() throws JsonProcessingException {
        RequestMQConfigResponseHeader header = new RequestMQConfigResponseHeader();
        MQConfig config = PojoHeaderConverter.responseHeader2MQConfig(header);
    }
}
