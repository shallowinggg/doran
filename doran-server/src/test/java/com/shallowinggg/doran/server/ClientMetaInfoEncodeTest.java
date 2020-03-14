package com.shallowinggg.doran.server;

import com.shallowinggg.doran.common.Domain;
import com.shallowinggg.doran.common.MqConfig;
import com.shallowinggg.doran.transport.protocol.RemotingSerializable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClientMetaInfoEncodeTest {

    @Test
    public void testEncodeMqConfigCollection() {
        ClientMetaInfo info = new ClientMetaInfo("test", "test");
        MqConfig mqConfig = new MqConfig();
        mqConfig.setName("mq_test");
        mqConfig.setUrls("127.0.0.1:1234");
        mqConfig.setUsername("admin");
        mqConfig.setPassword("admin");
        mqConfig.setDomain(Domain.PTP);
        mqConfig.setDomainName("queue_test");
        mqConfig.setThreadNum(2);
        mqConfig.setTimestamp(System.currentTimeMillis());
        info.addMqConfig(mqConfig);

        byte[] json = RemotingSerializable.encode(info.holdingConfigs());
        List<MqConfig> origin = RemotingSerializable.decodeArray(json, MqConfig.class);
        Collection<MqConfig> actual = Collections.unmodifiableCollection(origin);

        Assert.assertEquals(info.holdingConfigs().toString(), actual.toString());
    }
}
