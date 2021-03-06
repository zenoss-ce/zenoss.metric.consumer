/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2017, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss distributed with this file.
 *
 * ***************************************************************************
 */
package org.zenoss.app.consumer.metric.zing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zenoss.app.consumer.metric.ZingConfiguration;
import org.zenoss.app.consumer.metric.ZingSender;
import org.zenoss.app.consumer.metric.data.Metric;

import java.util.Collection;


/**
 * Pulls from a queue of metrics and sends batches of them to Zing.
 */
@Component
public class ZingWriter implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ZingWriter.class);

    private ZingConfiguration zingConfiguration;

    /**
     * The queue containing metrics to send
     */
    private ZingQueue zingQueue;

    /**
     * used to register/track writer threads
     */
    private final ZingWriterRegistry writerRegistry;

    /**
     * Used to send a batch of metrics to the Zing.
     */
    private final ZingSender sender;

    /**
     * Size of batches to send to the Zing socket
     */
    private final int batchSize;

    /**
     * Max idle time before suicide
     */
    private final int maxIdleTime;

    /**
     * Is this instance currently running?
     */
    private transient boolean running;

    /**
     * Has this instance been canceled?
     */
    private transient boolean canceled;

    /**
     * Last time this instance did work
     */
    protected transient long lastWorkTime;

    @Autowired
    ZingWriter(ZingConfiguration config,
               ZingWriterRegistry registry,
               ZingQueue zingQueue,
               ZingSender sender) {
        this.zingConfiguration = config;
        this.writerRegistry = registry;
        this.zingQueue = zingQueue;
        this.sender= sender;

        this.batchSize = zingConfiguration.getBatchSize();
        this.maxIdleTime = zingConfiguration.getMaxIdleTime();
        this.running = false;
        this.canceled = false;
        this.lastWorkTime = 0;
    }

    @Override
    public void run() {
        log.info("Starting writer");
        try {
            writerRegistry.register(this);
            running = true;
            runUntilCanceled();
        } catch (InterruptedException ie) {
            log.info("Exiting due to thread interrupt");
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.error("Thread exiting due to unexpected exception", e);
            throw e;
        } finally {
            running = false;
            writerRegistry.unregister(this);
        }
    }

    void runUntilCanceled() throws InterruptedException {

        while (!isCanceled()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Collection<Metric> metrics = zingQueue.poll(batchSize, maxIdleTime);
            log.debug("Back from polling zingQueue. metrics.size = {}",
                    null == metrics ? "null" : metrics.size());
            // Check to see if we should down this writer entirely.
            log.debug("Checking for shutdown. lastWorkTime = {}; maxIdleTime = {}; sum = {}; currentTime ={}",
                    lastWorkTime, maxIdleTime, lastWorkTime + maxIdleTime, System.currentTimeMillis());
            if (isNullOrEmpty(metrics) && // No records could be read from the
                    // metrics queue
                    lastWorkTime > 0 && // This thread has done work at least
                    // once
                    maxIdleTime > 0 && // The max idle time is set to something
                    // meaningful
                    System.currentTimeMillis() > lastWorkTime + maxIdleTime) // The
            // max
            // idle
            // time
            // has
            // expired
            {
                log.info("Shutting down writer due to dearth of work");
                break;
            }

            /*
             * If all the conditions were not met for shutting this writer down,
             * we still might want to just abort this run if we didn't get any
             * data from the metrics queue
             */
            if (isNullOrEmpty(metrics)) {
                log.debug("No work to do, so checking again.");
                continue;
            }

            // We have some work to do, some process what we got from the
            // metrics queue
            processBatch(metrics);
        }
        log.debug("work canceled.");
    }

    private boolean isNullOrEmpty(Collection<Metric> metrics) {
        return null == metrics || metrics.isEmpty();
    }

    void processBatch(Collection<Metric> metrics) {
        try {
            log.trace("processBatch, size={}, batch={}", metrics.size(), metrics);
            sender.send(metrics);

        } catch (Exception e) {
            // TODO: Should we requeue the batch of metrics at this point?
            //       Otherwise, the batch is lost
            log.warn("Caught exception while processing metrics: {}", e.getMessage());

        } finally {
            lastWorkTime = System.currentTimeMillis();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private synchronized boolean isCanceled() {
        return canceled;
    }

    public synchronized void cancel() {
        log.info("Writer shutdown requested");
        this.canceled = true;
    }

}
