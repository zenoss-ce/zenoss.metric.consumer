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


import org.zenoss.dropwizardspring.annotations.Resource;

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Resource(name = "status")
@Path("/ping/status")
public class StatusResource {

    @HEAD
    public Response status() {
        return Response.ok().entity(String.valueOf(true)).build();
    }

}
