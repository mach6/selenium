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

import static com.google.common.net.MediaType.JSON_UTF_8;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;

import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RestApiEndpoint extends RegistryBasedServlet {
  private Map<String, Method> getMethods = Collections.emptyMap();
  private Map<String, Method> postMethods = Collections.emptyMap();
  private Map<String, Method> deleteMethods  = Collections.emptyMap();
  private Map<String, Method> putMethods = Collections.emptyMap();

  RestApiEndpoint() {
    this(null);
  }

  RestApiEndpoint(Registry registry) {
    super(registry);

    getMethods = reflectMethods(RestGet.class);
    postMethods = reflectMethods(RestPost.class);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (getMethods.isEmpty()) {
      super.doGet(req, resp);
      return;
    }

    process(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (postMethods.isEmpty()) {
      super.doPost(req, resp);
      return;
    }

  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (putMethods.isEmpty()) {
      super.doPut(req, resp);
      return;
    }

  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (deleteMethods.isEmpty()) {
      super.doDelete(req, resp);
      return;
    }

  }

  protected void process(HttpServletRequest request, HttpServletResponse response)
    throws IOException {

    response.setContentType(JSON_UTF_8.toString());
    response.setCharacterEncoding("UTF-8");
    response.setStatus(200);

    try {
      response.getWriter().print(new GsonBuilder().setPrettyPrinting().create()
                                     .toJson(getResponse(request.getPathInfo())));
    } catch (RuntimeException e) {
      throw new GridException(e.getMessage());
    } finally {
      response.getWriter().close();
    }
  }

  public abstract Object getResponse(String query);

  boolean isInvalidQuery(String query) {
    return query == null || "/".equals(query) ;
  }

  private List<Method> getRestMethodsByType(Class<?> annotation) {
    return Arrays.stream(this.getClass().getDeclaredMethods())
        .filter(m -> Arrays.stream(m.getDeclaredAnnotations())
            .anyMatch(a -> a.annotationType() == annotation))
        .collect(Collectors.toList());
  }

  private Map<String, Method> getRestPaths(List<Method> methods) {
    if (methods.isEmpty()) {
      return Collections.emptyMap();
    }

    // if there is only one method of the type (GET, POST, etc)
    // it does not have to define a RestPath
    if (methods.size() == 1) {
      Method m = methods.get(0);
      RestPath rp = m.getAnnotation(RestPath.class);
        return (rp == null) ?
               ImmutableMap.of("", m) :
               ImmutableMap.of(rp.path(), m);
    }

    // go through all the methods and collect them.
    // any two or methods that use the same path will
    // result in only one being used.
    return methods
        .stream()
        .filter(m -> m.getAnnotation(RestPath.class) != null)
        .collect(Collectors.toMap(
            a -> a.getAnnotation(RestPath.class).path(),
            a -> a
        ));
  }

  private Map<String, Method> reflectMethods(Class<?> annotation) {
    return getRestPaths(getRestMethodsByType(annotation));
  }


}
