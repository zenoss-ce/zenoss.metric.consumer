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

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ExecutorService;
import org.zenoss.app.consumer.metric.MetricServiceConfiguration;
import org.zenoss.app.consumer.metric.TsdbWriter;
import org.zenoss.app.consumer.metric.TsdbWriterRegistry;
import org.zenoss.app.consumer.metric.data.Control;
import static org.mockito.Mockito.*;
/**
 *
 * @author cschellenger
 */
public class OpenTsdbWriterManagerTest {
 
    ApplicationContext context;
    MetricServiceConfiguration config;
    EventBus eventBus;
    ExecutorService executorService;
    TsdbWriterRegistry registry;
    
    @Before
    public void setUp() {
        context = mock(ApplicationContext.class);
        config = new MetricServiceConfiguration();
        eventBus = mock(EventBus.class);
        executorService = mock(ExecutorService.class);
        registry = mock(TsdbWriterRegistry.class);
    }
    
    OpenTsdbWriterManager createService() {
        return new OpenTsdbWriterManager(context, config, eventBus, executorService, registry);
    }
    
    void nothingHappens() {
        verify(context, never()).getBean(TsdbWriter.class);
        verify(registry, never()).size();
    }
    
    @Test
    public void testStartAllWritersError() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (0);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.error("test"));
        
        verify (executorService, times(3)).submit(writer);
    }
    
    @Test
    public void testStartAllWritersDropped() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (0);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.dropped("test"));
        
        verify (executorService, times(3)).submit(writer);
    }

    @Test
    public void testStartSomeWritersError() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (1);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);

        OpenTsdbWriterManager service = createService();
        service.processControl(Control.error("test"));

        verify (executorService, times(2)).submit(writer);
    }

    @Test
    public void testStartSomeWritersDropped() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (1);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);

        OpenTsdbWriterManager service = createService();
        service.processControl(Control.dropped("test"));

        verify (executorService, times(2)).submit(writer);
    }

    @Test
    public void testStartSomeWritersData() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (1);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.dataReceived());
        
        verify (executorService, times(2)).submit(writer);
    }
    

    @Test
    public void testRunTwiceFast() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (1);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.dataReceived());
        service.processControl(Control.dataReceived());
        
        verify (executorService, times(2)).submit(writer);
    }
    
    @Test
    public void testRunTwiceFastDroppedError() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (1);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.dropped("test"));
        service.processControl(Control.error("test"));
        
        verify (executorService, times(2)).submit(writer);
    }
    
    @Test
    public void testStartNoWritersError() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (3);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.error("test"));
        
        verify (executorService, never()).submit(writer);
    }
    
    @Test
    public void testStartNoWritersDropped() {
        config.setTsdbWriterThreads(3);
        TsdbWriter writer = mock(TsdbWriter.class);
        when (registry.size()).thenReturn (3);
        when (context.getBean(TsdbWriter.class)).thenReturn (writer);
        
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.dropped("test"));
        
        verify (executorService, never()).submit(writer);
    }
    
    @Test
    public void testHandleBufferUpdate() {
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.bufferUpdate(1));
        nothingHappens();
    }
    
    @Test
    public void testHandleMalformed() {
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.malformedRequest(null));
        nothingHappens();
    }
    
    @Test
    public void testHandleOK() {
        OpenTsdbWriterManager service = createService();
        service.processControl(Control.ok());
        nothingHappens();
    }
    
}
