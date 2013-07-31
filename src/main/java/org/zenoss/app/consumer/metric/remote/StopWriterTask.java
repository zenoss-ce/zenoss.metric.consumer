/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2013, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss under the directory where your Zenoss product is installed.
 *
 * ***************************************************************************
 */
package org.zenoss.app.consumer.metric.remote;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.primitives.Ints;
import com.yammer.dropwizard.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

import org.zenoss.app.consumer.metric.MetricService;

/**
 * This task asks the metric service to stop one ore more TSDB writer threads.
 */
@Component
public class StopWriterTask extends Task {

    @Autowired
    public StopWriterTask(MetricService metricService) {
        super("stop-writer");
        this.metricService = metricService;
        this.taskIntParser = new TaskIntParser();
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
        final int threads = taskIntParser.parse(parameters.get("threads"), 1);
        for (int i=0; i < threads; i++) {
            metricService.stopWriter();
        }
    }
    
    private final MetricService metricService;
    private final TaskIntParser taskIntParser;
}
