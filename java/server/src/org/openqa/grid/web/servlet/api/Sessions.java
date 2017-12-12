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

import com.google.gson.JsonObject;

import org.openqa.grid.internal.TestSession;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestPath(path = "sessions",
    description = "Returns details for sessions connected to the hub.")
public class Sessions extends RestApiEndpoint {

  @RestGet
  @RestPath(description = "Get all sessions")
  public RestResponse getSessions() {
    return new RestResponse()
        .setEntity(getAllSessions())
        .ok();
  }

  @RestGet
  @RestPath(path = "{id}",
      description = "Get a specific session using its id.")
  public RestResponse getSession(@RestPathParam("id") String sessionId) {
     Map<String, Object> sessionInfo = Collections.emptyMap();

    TestSession session = getRegistry().getActiveSessions()
        .stream()
        .filter(s -> s.getExternalKey().getKey().equals(sessionId))
        .findFirst()
        .orElse(null);

    if (session == null) {
      return new RestResponse()
        .setStatus(404);
    }

    sessionInfo.put("isOrphaned", session.isOrphaned());
    sessionInfo.put("internalKey", session.getInternalKey());
    sessionInfo.put("requestedCapabilities", session.getRequestedCapabilities());
    sessionInfo.put("isForwardingRequest", session.isForwardingRequest());
    sessionInfo.put("protocol", session.getSlot().getProtocol());
    sessionInfo.put("lastActivityWasAt",session.getInactivityTime());
    sessionInfo.put("requestPath", session.getSlot().getPath());
    JsonObject proxy = new JsonObject();
    URL url = session.getSlot().getProxy().getRemoteHost();
    proxy.addProperty("host", url.getHost());
    proxy.addProperty("port", url.getPort());
    proxy.addProperty("nodeId", session.getSlot().getProxy().getId());
    sessionInfo.put("proxy", proxy);

    return new RestResponse()
      .setEntity(sessionInfo)
      .ok();
  }

  private Map<String, JsonObject> getAllSessions() {
    Map<String, JsonObject> sessions = new HashMap<>();
    Set<TestSession> activeSessions = this.getRegistry().getActiveSessions();
    for (TestSession session : activeSessions) {
      JsonObject sessionData = new JsonObject();
      sessionData.add("slotInfo", ProxyUtil.getNodeInfo(session.getSlot().getProxy()));
      String browser = (String) ProxyUtil.getBrowser(session.getRequestedCapabilities());
      if (browser != null && !browser.trim().isEmpty()) {
        sessionData.addProperty("browser", browser);
      }
      sessions.put(session.getExternalKey().getKey(), sessionData);
    }
    return sessions;
  }
}
