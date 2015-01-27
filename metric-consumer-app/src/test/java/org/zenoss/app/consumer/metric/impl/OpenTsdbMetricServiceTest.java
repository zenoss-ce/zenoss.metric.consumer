/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2013, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss distributed with this file.
 *
 * ***************************************************************************
 */
package org.zenoss.app.consumer.metric.impl;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;

import org.zenoss.app.consumer.metric.MetricServiceConfiguration;
import org.zenoss.app.consumer.metric.data.Control;
import org.zenoss.app.consumer.metric.data.Metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class OpenTsdbMetricServiceTest {

    MetricServiceConfiguration config;
    EventBus eventBus;
    MetricsQueue metricsQueue;

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        config = new MetricServiceConfiguration();
        metricsQueue = mock(MetricsQueue.class);
    }
    
    OpenTsdbMetricService newService() {
        return new OpenTsdbMetricService(config, eventBus, metricsQueue);
    }

    @Test
    public void testPushHandlesNull() throws Exception {
        List<Metric> metrics  = Collections.singletonList(new Metric("name", 0, 0.0));
        OpenTsdbMetricService service = newService();
        assertEquals(Control.malformedRequest("metrics not nullable"), service.push(null, "test", null));
        assertEquals(Control.malformedRequest("clientId not nullable"), service.push(metrics, null, null));
    }

    @Test
    public void testPush() throws Exception {
        Metric metric = new Metric("name", 0, 0.0);
        List<Metric> metrics  = Collections.singletonList(metric);
        OpenTsdbMetricService service = newService();
        assertEquals(Control.ok(), service.push(metrics, "test", null));
        verify(metricsQueue, times(1)).addAll(metrics, "test");
    }

    @Test
    public void testPushWithOverflow() throws Exception {
        Metric metric = new Metric("name", 0, 0.0);
        List<Metric> metrics  = Collections.singletonList(metric);
        config.setJobSize(Integer.MAX_VALUE);
        OpenTsdbMetricService service = newService();
        assertEquals(Control.ok(), service.push(metrics, "test", null));
        verify(metricsQueue, times(1)).addAll(metrics, "test");
    }

    @Test
    public void testPushCollision() throws Exception {
        Metric metric = new Metric("name", 0, 0.0);
        List<Metric> metricList = Lists.newArrayList(metric, metric);
        config.setMaxQueueSize(3);
        config.setMaxClientWaitTime(1);
        when(metricsQueue.getTotalInFlight()).thenReturn(3L);
        OpenTsdbMetricService service = newService();
        assertEquals(Control.dropped("consumer is overwhelmed"), service.push(metricList,"test", null));
        verify(metricsQueue, never()).addAll(metricList, "test");
    }
}
