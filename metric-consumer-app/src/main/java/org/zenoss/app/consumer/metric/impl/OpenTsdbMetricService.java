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

import com.google.api.client.util.ExponentialBackOff;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.zenoss.app.consumer.metric.BufferListener;
import org.zenoss.app.consumer.metric.MetricService;
import org.zenoss.app.consumer.metric.MetricServiceConfiguration;
import org.zenoss.app.consumer.metric.TsdbMetricsQueue;
import org.zenoss.app.consumer.metric.data.Control;
import org.zenoss.app.consumer.metric.data.Metric;

import java.io.IOException;
import java.util.List;

@Component
class OpenTsdbMetricService implements MetricService {

    private static final Logger log = LoggerFactory.getLogger(OpenTsdbMetricService.class);

    @Autowired
    OpenTsdbMetricService(
            MetricServiceConfiguration config,
            @Qualifier("zapp::event-bus::async") EventBus eventBus,
            MetricsQueue metricsQueue) {
        // Dependencies
        this.eventBus = eventBus;
        this.metricsQueue = metricsQueue;

        // Configuration
        this.maxQueueSize = config.getMaxQueueSize();
        this.maxClientWaitTime = config.getMaxClientWaitTime();
        this.maxTimeBetweenRetries = config.getMaxTimeBetweenRetries();
        this.bufferUpdateInterval = config.getBufferUpdateInterval();
    }

    @Override
    public void incrementReceived(long received) {
        metricsQueue.incrementReceived(received);
    }

    @Override
    public Control push(final List<Metric> metrics, final String clientId, BufferListener bufferListener) {
        if (metrics == null) {
            return Control.malformedRequest("metrics not nullable");
        }
        if (clientId == null) {
            metricsQueue.incrementRejected(metrics.size());
            log.info("Rejected: [{}] clientId not nullable", metrics.size());
            return Control.malformedRequest("clientId not nullable");
        }
        long maxPushSize = maxQueueSize - 1;
        if (metrics.size() > maxPushSize) {
            String reason = String.format("cannot push more than %d metrics at a time", maxPushSize);
            metricsQueue.incrementRejected(metrics.size());
            log.info("Rejected: [{}] {}", metrics.size(), reason);
            return Control.malformedRequest(reason);
        }
        long size = metrics.size();

        if (keepsColliding(size, clientId, bufferListener)) {
            log.info("Rejected: [{}] consumer is overwhelmed", metrics.size());
            metricsQueue.incrementRejected(metrics.size());
            return Control.dropped("consumer is overwhelmed");
        }

        if (size > 0) {
            metricsQueue.addAll(Lists.newArrayList(metrics), clientId);
            eventBus.post(Control.dataReceived());
            return Control.ok();
        } else {
            return Control.dropped("empty request");
        }
    }

    /**
     * Checks {@link #availableBuffer(String)} until it returns enough for the incomingSize, or we give up.
     * Periodic checks are spaced out using an exponential back off.
     * @param incomingSize the number of metrics being added
     * @param clientId an identifier for the source of the metrics
     * @return false if and only if {@link #availableBuffer(String)} returned a number big enough for incomingSize before we gave up.
     */
    private boolean keepsColliding(final long incomingSize, final String clientId, BufferListener bufferListener) {
        ExponentialBackOff backOffTracker = null;
        long retryAt = 0L;
        long sendBufferUpdateAt = 0L;
        long backOff;
        long now;
        int collisions = 0;
        while (true) {
            now = System.currentTimeMillis();
            if (now > sendBufferUpdateAt || now > retryAt) {
                long available = availableBuffer(clientId);
                if (bufferListener != null && now > sendBufferUpdateAt) {
                    bufferListener.onBufferUpdate(clientId, available - incomingSize);
                    sendBufferUpdateAt = now + bufferUpdateInterval;
                }
                if (now > retryAt) {
                    if (incomingSize > available) {
                        metricsQueue.incrementClientCollision();
                        collisions++;
                        if (backOffTracker == null) {
                            backOffTracker = buildExponentialBackOff();
                        }
                        try {
                            backOff = backOffTracker.nextBackOffMillis();
                            retryAt = now + backOff;
                        } catch (IOException e) {
                            // should never happen
                            log.error("Caught IOException backing off tracker.", e);
                            throw new RuntimeException(e);
                        }
                        long elapsed = backOffTracker.getElapsedTimeMillis();
                        if (ExponentialBackOff.STOP == backOff) {
                            log.warn("Too many collisions ({}). Gave up after {}ms.", collisions, elapsed);
                            return true;
                        } else {
                            log.debug("Collision detected ({} in {}ms). Backing off for {} ms", collisions, elapsed, backOff);
                        }
                    } else {
                        return false;
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for a spot in the queue.");
                Thread.currentThread().interrupt();
                return true;
            }
        }
    }

    private ExponentialBackOff buildExponentialBackOff() {
        return new ExponentialBackOff.Builder().
            setInitialIntervalMillis(1).
            setMaxIntervalMillis(maxTimeBetweenRetries).
            setMaxElapsedTimeMillis(maxClientWaitTime).
            build();
    }

    private long availableBuffer(final String clientId) {
        final long totalInFlight = metricsQueue.getTotalInFlight();
        final long clientInFlight = metricsQueue.clientBacklogSize(clientId);
        final long toGlobalCollision = maxQueueSize - totalInFlight;
        final long toClientCollision = clientCollisionMark() - clientInFlight;
        return Math.min(toGlobalCollision, toClientCollision);
    }

    private long clientCollisionMark() {
        long clientCount = Math.max(1, metricsQueue.clientCount());
        long result = 9 * maxQueueSize / (clientCount * 10);
        return Math.max(64, result);
    }

    /**
     * event bus for broadcasts
     */
    private final EventBus eventBus;

    /**
     * Shared data structure holding metrics to be pushed into TSDB
     */
    private final TsdbMetricsQueue metricsQueue;

    /**
     * high collision detection mark
     */
    private final long maxQueueSize;

    /**
     * maximum time for a client to wait to add metrics to a backlogged queue before giving up.
     */
    private final int maxClientWaitTime;

    /**
     * Maximum time between retries to accept metrics into a back-logged metric queue.
     */
    private final int maxTimeBetweenRetries;

    /**
     * Number of milliseconds between callbacks to the BufferListener.
     */
    private final int bufferUpdateInterval;
}
