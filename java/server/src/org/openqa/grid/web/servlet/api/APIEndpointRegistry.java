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

import com.google.common.collect.ImmutableList;

import org.openqa.grid.web.servlet.ProxyStatusServlet;
import org.openqa.grid.web.servlet.TestSessionStatusServlet;

import java.util.List;

public class APIEndpointRegistry {
  public static final class EndPoint {
    private String path;
    private String description;
    private String className;
    private String usage;

    EndPoint(String pathPrefix, Class<?> clazz) {
      RestPath rp = clazz.getAnnotation(RestPath.class);
      if (rp != null) {
        this.className = clazz.getName();
        this.path = pathPrefix + ((rp.path().toCharArray()[0] == '/') ? "" : "/") + rp.path();
        this.description = rp.description();
        this.usage = rp.usage();
      }
    }

    EndPoint(String path, String description, String className, String usage) {
      this.path = path;
      this.description = description;
      this.className = className;
      this.usage = usage;
    }

    EndPoint(String path, String description, String className) {
      this(path, description, className, null);
    }

    public String getDescription() {
      return this.description;
    }

    String getPath() {
      return this.path;
    }

    public String getPathSpec() {
      return getPath() + "/*";
    }

    public String getClassName() {
      return this.className;
    }

    public String getUsage() {
      if (this.usage == null) {
        return getPath();
      } else {
        return this.usage;
      }
    }
  }

  private static final String API_PREFIX = "/grid/api";
  private static final String BETA_API_PREFIX = API_PREFIX + "/beta";

  private static final List<EndPoint> endpoints = ImmutableList.of(
    new EndPoint(API_PREFIX, "HTML page documenting the API endpoints (this page)",
                 ApiV1.class.getName()),

    new EndPoint(BETA_API_PREFIX, HubInfo.class),
    new EndPoint(BETA_API_PREFIX, Proxy.class),
    new EndPoint(BETA_API_PREFIX, Sessions.class),

    new EndPoint(API_PREFIX + "/proxy", "Returns registration information for a proxy.",
                 ProxyStatusServlet.class.getName(), API_PREFIX + "/proxy?id=&lt;proxyId&gt;"),

    new EndPoint(API_PREFIX + "/testsession", "Returns status information for a test session.",
                 TestSessionStatusServlet.class.getName(),
                 API_PREFIX + "/testsession?session=&lt;sessionId&gt;")
  );

  private APIEndpointRegistry() {
    //defeat instantiation.
  }

  public static List<EndPoint> getEndpoints() {
    return endpoints;
  }
}
