package com.shallowinggg.doran.common.util;

import com.shallowinggg.doran.common.MQConfig;
import com.shallowinggg.doran.common.RequestMQConfigResponseHeader;
import org.junit.Test;

import java.io.IOException;

public class PojoHeaderConverterTest {

    @Test
    public void test() throws IOException {
        RequestMQConfigResponseHeader header = new RequestMQConfigResponseHeader();
        MQConfig config = PojoHeaderConverter.responseHeader2MQConfig(header);
    }
}
