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
package org.zenoss.app.consumer.metric.data;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.yammer.dropwizard.testing.JsonHelpers.asJson;
import static com.yammer.dropwizard.testing.JsonHelpers.fromJson;
import static com.yammer.dropwizard.testing.JsonHelpers.jsonFixture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageTest {

    @Test
    public void serializesToJSON() throws Exception {
        Control control = new Control( );
        Map<String, String> tags = new HashMap<>();
        tags.put( "tagName", "tagValue");
        Metric metric = new Metric("metric", 0, 0.0, tags);
        Message message = new Message( control, new Metric[]{ metric});
        assertThat(asJson(message), is(jsonFixture("fixtures/message.json")));
    }


    @Test
    public void deserializesFromJSON() throws Exception {
        Control control = new Control( );
        Map<String, String> tags = new HashMap<>();
        tags.put( "tagName", "tagValue");
        Metric metric = new Metric("metric", 0, 0.0, tags);
        Message message = new Message( control, new Metric[]{ metric});
        assertThat(fromJson(jsonFixture("fixtures/message.json"), Message.class), is(message));
    }
}
