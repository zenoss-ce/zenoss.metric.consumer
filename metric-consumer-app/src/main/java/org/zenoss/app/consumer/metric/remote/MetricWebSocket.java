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
package org.zenoss.app.consumer.metric.remote;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.shiro.subject.Subject;
import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.consumer.metric.BufferListener;
import org.zenoss.app.consumer.metric.data.BinaryDecoder;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.consumer.ConsumerAppConfiguration;
import org.zenoss.app.consumer.metric.MetricService;
import org.zenoss.app.consumer.metric.data.Control;
import org.zenoss.app.consumer.metric.data.Message;
import org.zenoss.app.consumer.metric.data.Metric;
import org.zenoss.dropwizardspring.websockets.WebSocketBroadcast;
import org.zenoss.dropwizardspring.websockets.WebSocketSession;
import org.zenoss.dropwizardspring.websockets.annotations.OnClose;
import org.zenoss.dropwizardspring.websockets.annotations.OnMessage;
import org.zenoss.dropwizardspring.websockets.annotations.WebSocketListener;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@WebSocketListener(name = "metrics/store")
@Path("/ws/metrics/store")
public class MetricWebSocket {

    private static final Logger log = LoggerFactory.getLogger(MetricWebSocket.class);

    private ConsumerAppConfiguration configuration;

    private final WeakHashMap<WebSocket.Connection, BinaryDecoder> decoders = new WeakHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final LoadingCache<WebSocketSession, String> connectionIds = CacheBuilder
            .newBuilder()
            .expireAfterAccess(6, TimeUnit.MINUTES)
            .weakKeys()
            .build(CacheLoader.from(new Supplier<String>() {
                @Override
                public String get() {
                    return String.format("websocket%d",sequence.incrementAndGet());
                }
            }));

    @Autowired
    public MetricWebSocket(ConsumerAppConfiguration configuration, MetricService service) {
        this.service = service;
        this.configuration = configuration;
    }

    @OnClose
    public void onClose(Integer closeCode, String message, WebSocketSession session) {
        decoders.remove(session.getConnection());
    }

    @OnMessage
    public Control onMessage(byte[] data, WebSocketSession session) {
        try {
            BinaryDecoder decoder = decoders.get(session.getConnection());
            if (decoder == null) {
                decoder = new BinaryDecoder();
                decoders.put(session.getConnection(), decoder);
            }
            try {
                return onMessage(decoder.decode(data), session);
            } catch (IOException e) {
                log.error("Invalid message");
                return Control.malformedRequest("Invalid message");
            }
        } catch (RuntimeException e) {
            log.error("Unexpected exception: " + e.getMessage(), e);
            return Control.error(e.getMessage());
        }
    }

    private String getClientId(WebSocketSession session) {
        return connectionIds.getUnchecked(session);
    }

    @OnMessage
    public Control onMessage(Message message, final WebSocketSession session) {
        try {
            Metric[] metrics = message.getMetrics();
            int metricsLength = (metrics == null) ? -1 : metrics.length;
            log.debug("Message(control={}, len(metrics)={}) - START", message.getControl(), metricsLength);

            //process metrics
            if (metrics != null) {
                service.incrementReceived(metrics.length);
                log.debug( "Tagging metrics with parameters: {}", configuration.getHttpParameterTags());
                HttpServletRequest request = session.getHttpServletRequest();

                //tag metrics using configured http parameters
                List<Metric> metricList = Arrays.asList(metrics);
                Utils.tagMetrics(request, metricList, configuration.getHttpParameterTags());

                //tag metrics with tenant id (obviously, tenant-id's identified through authentication)
                if (configuration.isAuthEnabled()) {
                    Subject subject = session.getSubject();
                    ZenossTenant tenant = subject.getPrincipals().oneByType(ZenossTenant.class);

                    log.debug("Tagging metrics with tenant_id: {}", tenant.id());
                    Utils.injectTag("zenoss_tenant_id", tenant.id(), metricList);
                }

                //filter tags using configuration white list
                Utils.filterMetricTags( metricList, configuration.getTagWhiteList());

                //enqueue metrics for transfer
                final String clientId = getClientId(session);
                Control control = service.push(metricList, clientId, bufferListener(session));
                log.debug("Message(control={}, len(metrics)={}) -> {}", message.getControl(), metricsLength, control);
                return control;
            } else {
                return Control.malformedRequest("Null metrics not accepted");
            }
        } catch (RuntimeException e) {
            log.error("Unexpected exception: " + e.getMessage(), e);
            return Control.error(e.getMessage());
        }
    }

    private BufferListener bufferListener(final WebSocketSession session) {
        return new BufferListener(){
            @Override
            public void onBufferUpdate(String clientId, long remainingBuffer) {
                clientBufferNotification(Control.bufferUpdate(remainingBuffer), session);
            }
        };
    }

    void clientBufferNotification(Control event, WebSocketSession session) {
        try {
            String message = WebSocketBroadcast.newMessage(getClass(), event).asString();
            session.sendMessage(message);
            log.debug("Buffer update sent: {}", message);
        } catch (IOException e) {
            log.warn("Failed to send buffer update to client: {}", e.getMessage());
        }
    }

    private final MetricService service;
}
