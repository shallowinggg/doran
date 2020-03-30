package com.shallowinggg.doran.client;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class MetricsTest {
    private MetricRegistry registry;

    @Before
    public void before() {
        registry = new MetricRegistry();
    }

    @Test
    public void testMeter() {
        Meter meter = registry.meter("test");
        AtomicInteger i = new AtomicInteger();
        new Thread(() -> {
            while (true) {
                int val = i.incrementAndGet();
                meter.mark();

                try {
                    if (val <= 60)
                        sleep(500);
                    else
                        sleep(300);
                } catch (InterruptedException e) {
                    //
                }
            }
        }).start();

        try {
            sleep(30 * 1000);
        } catch (InterruptedException e) {
            //
        }
        System.out.println(meter.getOneMinuteRate());
        System.out.println(meter.getCount());

        try {
            sleep(30 * 1000);
        } catch (InterruptedException e) {
            //
        }
        System.out.println(meter.getOneMinuteRate());
        System.out.println(meter.getCount());
    }
}
