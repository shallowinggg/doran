package com.shallowinggg.doran.common;

import com.shallowinggg.doran.common.util.JarDependent;
import org.junit.Test;

public class JarDependentTest {

    @Test
    public void testMQType() {
        MQType type = JarDependent.mqType();
        System.out.println(type);
        assert type != null;


    }
}
