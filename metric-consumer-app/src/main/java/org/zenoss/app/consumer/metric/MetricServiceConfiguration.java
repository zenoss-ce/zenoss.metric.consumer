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

package org.zenoss.app.consumer.metric;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.zenoss.lib.tsdb.OpenTsdbClientPoolConfiguration;

import javax.validation.Valid;

import org.zenoss.app.consumer.metric.data.Control.Type;

public class MetricServiceConfiguration {

    @Valid
    @JsonProperty("openTsdbClientPool")
    private OpenTsdbClientPoolConfiguration openTsdbClientPoolConfiguration = new OpenTsdbClientPoolConfiguration();

    /**
     * how many metrics per thread
     */
    @JsonProperty
    private int jobSize = 5;

    /**
     * How many queued messages before throttling/dropping metrics from all clients
     */
    @JsonProperty
    private long maxQueueSize = 1024;

    /**
     * Max time in milliseconds to have a throttled client wait to add some metrics to a backlogged queue
     * before we decide to just give up instead.
     */
    @JsonProperty
    private int maxClientWaitTime = 60000;

    /**
     * Maximum time between retries to accept metrics into a back-logged metric queue.
     */
    @JsonProperty
    private int maxTimeBetweenRetries = 3000;

    /**
     * Number of milliseconds between callbacks to the BufferListener.
     */
    @JsonProperty
    private int bufferUpdateInterval = 100;

    /**
     * Max time in milliseconds with no work before TSDB writer threads will commit seppuku
     */
    @JsonProperty
    private int maxIdleTime = 10000;

    /**
     * Max time in milliseconds to wait for reconnecting when no connections available
     */
    @JsonProperty
    private int maxConnectionBackOff = 5000;

    /**
     * Min time in milliseconds to wait for reconnecting when no connections available
     */
    @JsonProperty
    private int minConnectionBackOff = 100;

    /**
     * Ideal number of TSDB writer threads
     */
    @JsonProperty
    private int tsdbWriterThreads = 1;

    /**
     * Size of tsdb writer thread pool
     */
    @JsonProperty
    private int threadPoolSize = 10;

    @JsonProperty
    private String consumerName = "Consumer";

    @JsonProperty
    private int selfReportFrequency = 0; // Zero means no reporting

    /**
     * TSDB client pool configuration.
     *
     * @return config
     */
    public OpenTsdbClientPoolConfiguration getOpenTsdbClientPoolConfiguration() {
        return openTsdbClientPoolConfiguration;
    }

    /**
     * The name of this consumer. This should be unique per JVM.
     *
     * @return consumerName
     */
    public String getConsumerName() {
        return consumerName;
    }

    /**
     * The maximum acceptable backlog of metrics. Once this threshold has been
     * reached, no further metrics will be accepted until some have been written
     * to TSDB.
     *
     * @return threshold
     */
    public long getMaxQueueSize() {
        return maxQueueSize;
    }

    /**
     * The batch size to use when writing metrics to TSDB.
     *
     * @return batch size
     */
    public int getJobSize() {
        return jobSize;
    }

    /**
     * The maximum time to have a throttled client wait to add some metrics to a backlogged queue before we decide
     * to just give up instead.
     *
     * @return milliseconds
     */
    public int getMaxClientWaitTime() {
        return maxClientWaitTime;
    }

    /**
     * Maximum time between retries to accept metrics into a back-logged metric queue.
     *
     * @return milliseconds
     */
    public int getMaxTimeBetweenRetries() {
        return maxTimeBetweenRetries;
    }

    /**
     * Number of milliseconds between callbacks to the BufferListener.
     *
     * @return milliseconds
     */
    public int getBufferUpdateInterval() {
        return bufferUpdateInterval;
    }

    /**
     * The maximum time TSDB writer threads should wait while there is no work
     * to do.
     *
     * @return milliseconds
     */
    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * The maximum time to wait before trying to get a new connection when one isn't available
     *
     * @return milliseconds
     */
    public int getMaxConnectionBackOff() {
        return maxConnectionBackOff;
    }

    /**
     * The minimum time to wait before trying to get a new connection when one isn't available
     *
     * @return milliseconds
     */
    public int getMinConnectionBackOff() {
        return minConnectionBackOff;
    }

    /**
     * The frequency with which this application will report internal metrics
     * on throughput to TSDB. If this is less than or equal zero the reporter
     * will be disabled.
     *
     * @return time in milliseconds
     */
    public int getSelfReportFrequency() {
        return selfReportFrequency;
    }

    /**
     * The minimum size of the general purpose thread pool for this application
     *
     * @return size
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * Number of background threads that will simultaneously write to TSDB.
     *
     * @return threads
     */
    public int getTsdbWriterThreads() {
        return tsdbWriterThreads;
    }

    /**
     * The name of this consumer. This should be unique per JVM.
     *
     * @param consumerName unique name
     */
    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    /**
     * The maximum acceptable backlog of metrics. Once this threshold has been
     * reached, no further metrics will be accepted until some have been written
     * to TSDB.
     *
     * @param maxQueueSize threshold
     */
    public void setMaxQueueSize(long maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * The batch size to use when writing metrics to TSDB.
     *
     * @param jobSize batch size
     */
    public void setJobSize(int jobSize) {
        this.jobSize = jobSize;
    }

    /**
     * The maximum time to have a throttled client wait to add some metrics to a backlogged queue before we decide
     * to just give up instead.

     * @param maxClientWaitTime milliseconds
     */
    public void setMaxClientWaitTime(int maxClientWaitTime) {
        this.maxClientWaitTime = maxClientWaitTime;
    }

    /**
     * Maximum time between retries to accept metrics into a back-logged metric queue.
     *
     * @param maxTimeBetweenRetries milliseconds
     */
    public void setMaxTimeBetweenRetries(int maxTimeBetweenRetries) {
        this.maxTimeBetweenRetries = maxTimeBetweenRetries;
    }

    /**
     * Number of milliseconds between callbacks to the BufferListener.
     *
     * @param bufferUpdateInterval milliseconds
     */
    public void setBufferUpdateInterval(int bufferUpdateInterval) {
        this.bufferUpdateInterval = bufferUpdateInterval;
    }

    /**
     * The maximum time TSDB writer threads should wait while there is no work
     * to do.
     *
     * @param maxIdleTime milliseconds
     */
    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    /**
     * TSDB client pool configuration.
     *
     * @param openTsdbClientPoolConfiguration
     *         config
     */
    public void setOpenTsdbClientPoolConfiguration(OpenTsdbClientPoolConfiguration openTsdbClientPoolConfiguration) {
        this.openTsdbClientPoolConfiguration = openTsdbClientPoolConfiguration;
    }

    /**
     * The frequency with which this application will report internal metrics
     * on throughput to TSDB. If this is less than or equal zero the reporter
     * will be disabled.
     *
     * @param milliseconds time in milliseconds
     */
    public void setSelfReportFrequency(int milliseconds) {
        this.selfReportFrequency = milliseconds;
    }

    /**
     * Time to sleep between checking for work when the metric backlog is empty.
     *
     * @param numberOfThreads threads
     */
    public void setTsdbWriterThreads(int numberOfThreads) {
        this.tsdbWriterThreads = numberOfThreads;
    }
}
