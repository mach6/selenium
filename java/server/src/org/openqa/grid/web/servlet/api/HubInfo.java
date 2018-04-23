// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.grid.web.servlet.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.openqa.grid.internal.RemoteProxy;

import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Intended to be a replacement for the so called {@link org.openqa.grid.web.servlet.HubStatusServlet}
 */
@Path("hub")
@ApiDoc("Returns configuration and proxy information of the hub.")
public class HubInfo extends RestApiEndpoint {

  @GET
  public Response getHubInfo() {
    JsonObject hubData = new JsonObject();
    hubData.add("configuration", getRegistry().getConfiguration().toJson());
    hubData.add("nodes", proxies());
    hubData.addProperty("registrationUrl", registrationUrl());
    hubData.addProperty("consoleUrl", consoleUrl());
    hubData.addProperty("newSessionRequestCount", getRegistry().getNewSessionRequestCount());
    hubData.addProperty("usedProxyCount", getRegistry().getUsedProxies().size());
    hubData.addProperty("totalProxyCount", getRegistry().getAllProxies().size());
    hubData.addProperty("activeSessionCount", getRegistry().getActiveSessions().size());
    hubData.add("slotCounts", getSlotCounts());
    hubData.addProperty( "success", true);

    return Response.ok(hubData).build();
  }

  private String registrationUrl() {
    return urlToString(getRegistry().getHub().getRegistrationURL());
  }

  private String consoleUrl() {
    return urlToString(getRegistry().getHub().getConsoleURL());
  }

  private String urlToString(URL url) {
    return String
        .format("%s://%s:%d%s", url.getProtocol(), url.getHost(), url.getPort(), url.getPath());
  }

  private JsonArray proxies() {
    JsonArray proxies = new JsonArray();
    for (RemoteProxy proxy : getRegistry().getAllProxies()) {
      proxies.add(ProxyUtil.getNodeInfo(proxy));
    }
    return proxies;
  }

  private JsonObject getSlotCounts() {
    int totalSlots = 0;
    int usedSlots = 0;

    for (RemoteProxy proxy : getRegistry().getAllProxies()) {
      totalSlots += Math.min(proxy.getMaxNumberOfConcurrentTestSessions(), proxy.getTestSlots().size());
      usedSlots += proxy.getTotalUsed();
    }
    JsonObject result = new JsonObject();
    result.addProperty("free", totalSlots - usedSlots);
    result.addProperty("total", totalSlots);
    return result;
  }

}
