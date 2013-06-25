package org.zenoss.app.consumer.metric;

import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.consumer.metric.data.Control;
import org.zenoss.app.consumer.metric.data.Message;
import org.zenoss.app.consumer.metric.data.Metric;
import org.zenoss.dropwizardspring.websockets.annotations.OnMessage;
import org.zenoss.dropwizardspring.websockets.annotations.WebSocketListener;

import javax.ws.rs.Path;

@WebSocketListener
@Path("/socket/metric")
public class MetricWebSocket {
    static final Logger log = LoggerFactory.getLogger(MetricWebSocket.class);

    @Autowired
    private MetricService service;

    @OnMessage
    public Control onMessage(Message message, Connection connection) {
        log.debug( "Message: {}", message);
        Metric[] metrics = message.getMetrics();

        for ( Metric metric : metrics) {
            service.push( metric);
            //TODO handle response
        }

        return new Control();
    }

    @SuppressWarnings({"unused"})
    public MetricWebSocket( ) {
    }

    public MetricWebSocket( MetricService service) {
        this.service = service;
    }
}