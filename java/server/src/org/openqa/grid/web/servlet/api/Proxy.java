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

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.BaseRemoteProxy;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.selenium.remote.CapabilityType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestPath(path = "proxies",
    description = "Returns configuration and capability information for proxies connected to the hub.")
public class Proxy extends RestApiEndpoint {

  //TODO experimental
  @RestPost
  @RestPath(description = "Add a proxy.")
  public RestResponse addProxy(GridNodeConfiguration proxyConfig) {
    RemoteProxy proxy = BaseRemoteProxy.getNewInstance(new RegistrationRequest(proxyConfig), getRegistry());
    getRegistry().add(proxy);

    return new RestResponse()
        .setStatus(201);
  }

  //TODO experimental
  @RestDelete
  @RestPath(path = "{id}", description = "Delete a proxy by its id.")
  public RestResponse deleteProxy(@RestPathParam("id") String id) {
    System.out.println("Deleting proxy " + id);
    RemoteProxy proxy = getRegistry().getProxyById(id);
    getRegistry().removeIfPresent(proxy);

    return new RestResponse()
        .ok();
  }

  @RestGet
  @RestPath(description = "Get all proxies.")
  public RestResponse getProxies() {
    return new RestResponse()
        .setEntity(allProxyInfo())
        .ok();
  }

  @RestGet
  @RestPath(path = "{id}", description = "Get a specific proxy using its id.")
  public RestResponse getProxy(@RestPathParam("id") String proxyId) {
    Map<String, Object> proxyInfo = Maps.newHashMap();

    if (proxyId == null || proxyId.trim().isEmpty()) {
      return new RestResponse().error();
    }

    RemoteProxy proxy = getRegistry().getProxyById(proxyId);
    if (proxy == null) {
      //Maybe user gave only the node ip and port
      proxy = getRegistry().getProxyById(getProxyId(proxyId));
    }

    proxyInfo.put("config", proxy.getConfig().toJson());
    proxyInfo.put("slotUsage", ProxyUtil.getSlotUsage(proxy));
    proxyInfo.put("percentUsed", proxy.getResourceUsageInPercent());
    proxyInfo.put("htmlRenderer", proxy.getHtmlRender().getClass().getCanonicalName());
    proxyInfo.put("lastSessionStart", proxy.getLastSessionStart());
    proxyInfo.put("isBusy", proxy.isBusy());
    JsonObject status = proxy.getStatus(); // TODO might fail
    addInfoFromStatusIfPresent(proxyInfo, "build", status);
    addInfoFromStatusIfPresent(proxyInfo, "os", status);
    addInfoFromStatusIfPresent(proxyInfo, "java", status);
    List<JsonObject> sessions = new LinkedList<>();
    for (TestSlot slot : proxy.getTestSlots()) {
      if ((slot == null) || (slot.getSession() == null)) {
        continue;
      }
      JsonObject session = new JsonObject();
      String key = CapabilityType.BROWSER_NAME;
      String browser = (String)slot.getSession().getRequestedCapabilities().get(key);
      if (browser != null) {
        session.addProperty(key, browser);
      }
      session.addProperty("sessionId", slot.getSession().getExternalKey().getKey());
      sessions.add(session);
    }
    proxyInfo.put("sessions", sessions);
    proxyInfo.put("slotUsage", ProxyUtil.getDetailedSlotUsage(proxy));

    return new RestResponse()
        .setEntity(proxyInfo)
        .ok();
  }

  private List<JsonObject> allProxyInfo() {
    ProxySet proxies = this.getRegistry().getAllProxies();
    List<JsonObject> computers = new LinkedList<>();
    for (RemoteProxy each : proxies) {
      computers.add(gatherComputerData(each));
    }
    return computers;
  }

  private static String getProxyId(String proxyToFind) {
    String[] parts = proxyToFind.split(":", 2);
    if (parts.length < 2) {
      return proxyToFind;
    }
    return "http://" + parts[0] + ":" + parts[1];
  }

  private static void addInfoFromStatusIfPresent(Map<String, Object> map, String key, JsonObject status) {
    if (!status.has("value")) {
      return;
    }
    JsonObject value = status.get("value").getAsJsonObject();
    if (! value.has(key)) {
      return;
    }
    map.put(key, value.get(key).getAsJsonObject());
  }

  private JsonObject gatherComputerData(RemoteProxy proxy) {
    JsonObject computer = ProxyUtil.getNodeInfo(proxy);
    computer.add("slotUsage", ProxyUtil.getDetailedSlotUsage(proxy));
    return computer;
  }


}
