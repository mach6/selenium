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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestApiEndpoint extends RegistryBasedServlet {
  private Map<String, Method> getMethods = Collections.emptyMap();
  private Map<String, Method> postMethods = Collections.emptyMap();
  private Map<String, Method> deleteMethods = Collections.emptyMap();
  private Map<String, Method> putMethods = Collections.emptyMap();

  private class RestDetails {
    final private Map<String, String> pathParamsMap = Maps.newHashMap();

    Method method;
    HttpServletRequest httpServletRequest;
    HttpServletResponse httpServletResponse;
    Object[] methodArgs;

    RestDetails(HttpServletRequest req, HttpServletResponse resp, Map<String, Method> methods)
        throws IOException {
      httpServletRequest = req;
      httpServletResponse = resp;
      getMethodMatchingRestPath(methods);

      if (method == null) {
        httpServletResponse.sendError(404);
        return;
      }

      if (method.getParameterCount() > 0) {
        methodArgs = new Object[method.getParameterCount()];
        buildPathParamsMap();
        fillMethodArgs();
      }
    }

    private void getMethodMatchingRestPath(Map<String, Method> methods) {
      final String pathInfo = httpServletRequest.getPathInfo();

      if (pathInfo == null && methods.containsKey("")) {
        method = methods.get("");
        return;
      }

      String[] pathTokens = pathInfo.toLowerCase().split("/");
      if (pathTokens.length == 0 && methods.containsKey("")) {
        method = methods.get("");
        return;
      }

      List<String> potentialKeyMatches = methods.keySet()
          .stream()
          .filter(k -> ("/" + k).split("/").length == pathTokens.length)
          .collect(Collectors.toList());

      if (potentialKeyMatches.size() == 1) {
        method = methods.get(potentialKeyMatches.get(0));
      }
    }

    private void fillMethodArgs() {
      int i = 0;
      for (Parameter p : method.getParameters()) {
        if (p.isAnnotationPresent(RestPathParam.class)) {
          //TODO check type.. don't cast.
          methodArgs[i] =
              p.getType().cast(pathParamsMap.get(p.getAnnotation(RestPathParam.class).value()));
        } else if (p.isAnnotationPresent(RestQueryParam.class)) {
          //TODO check type.. don't cast.
          methodArgs[i] =
              p.getType().cast(httpServletRequest.getParameterMap().get(p.getAnnotation(RestQueryParam.class).value()));
        } else if (p.isAnnotationPresent(RestHeaderParam.class)) {
          final String headerVal =
              httpServletRequest.getHeader(p.getAnnotation(RestHeaderParam.class).value());
          //TODO check type.. don't cast.
          methodArgs[i] = p.getType().cast(headerVal);
        } else if (p.getType() == HttpServletRequest.class) {
          methodArgs[i] = httpServletRequest;
        } else if (p.getType() == HttpServletResponse.class) {
          methodArgs[i] = httpServletResponse;
        } else if (method.isAnnotationPresent(RestPost.class)
                  || method.isAnnotationPresent(RestPut.class)){
          Class<?> parameterType = p.getType();
          try {
            //TODO Clean up and handler more deserializers
            Method deserializer = parameterType.getMethod("loadFromJSON", JsonObject.class);
            if (deserializer != null) {
              methodArgs[i] = deserializer.invoke(this, getRequestJSON());
            }
            deserializer = parameterType.getMethod("fromJson", String.class);
            if (deserializer != null) {
              methodArgs[i] = deserializer.invoke(this, getRequestJSON());
            }
          } catch (NoSuchMethodException | IOException | InvocationTargetException | IllegalAccessException e) {
            //ignore
          }
        }
        i += 1;
      }
    }

    private JsonObject getRequestJSON() throws IOException {
      String requestJsonString;
      try (BufferedReader rd =
               new BufferedReader(new InputStreamReader(httpServletRequest.getInputStream()))) {
        requestJsonString = CharStreams.toString(rd);
      }
      return new JsonParser().parse(requestJsonString).getAsJsonObject();
    }

    private void buildPathParamsMap() {
      final RestPath restPath;
      if (method.isAnnotationPresent(RestPath.class)) {
        restPath = method.getAnnotation(RestPath.class);
      } else {
        return;
      }

      final String restPathValue = restPath.path();
      final String pathInfo = httpServletRequest.getPathInfo();

      if (pathInfo == null || pathInfo.isEmpty()
          || restPathValue == null || restPathValue.isEmpty())  {
        return;
      }

      String[] rpt = ("/" + restPathValue).split("/");
      String[] pvt = pathInfo.split("/");

      // should not happen.. wouldn't know how to handle it.
      if (rpt.length != pvt.length) {
        return;
      }

      for (int i = 0; i < rpt.length; i+=1) {
        if  (rpt[i].matches("^\\{(.*)\\}")) {
          pathParamsMap.put(rpt[i]
                                .replace("{", "")
                                .replace("}", ""),
                            pvt[i]);
        }
      }
    }

  }

  RestApiEndpoint() {
    this(null);
  }

  RestApiEndpoint(GridRegistry registry) {
    super(registry);

    getMethods = reflectMethods(RestGet.class);
    postMethods = reflectMethods(RestPost.class);
    deleteMethods = reflectMethods(RestDelete.class);
    putMethods = reflectMethods(RestPut.class);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (getMethods.isEmpty()) {
      super.doGet(req, resp);
      return;
    }
   process(new RestDetails(req, resp, getMethods));
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (postMethods.isEmpty()) {
      super.doPost(req, resp);
      return;
    }
    process(new RestDetails(req, resp, postMethods));
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (putMethods.isEmpty()) {
      super.doPut(req, resp);
      return;
    }
    process(new RestDetails(req, resp, putMethods));
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (deleteMethods.isEmpty()) {
      super.doDelete(req, resp);
      return;
    }
    process(new RestDetails(req, resp, deleteMethods));
  }

  protected void process(RestDetails details)
    throws IOException {

    try {
      Object invokeResponse = details.method.invoke(this, details.methodArgs);

      if (details.method.getReturnType() == RestResponse.class) {
        RestResponse restResponse = (RestResponse) invokeResponse;
        if (restResponse.getStatus() == 500) {
          details.httpServletResponse.sendError(500);
          return;
        }
        details.httpServletResponse.setContentType(restResponse.getContentType());
        details.httpServletResponse.setCharacterEncoding(restResponse.getEncoding());
        details.httpServletResponse.setStatus(restResponse.getStatus());
        restResponse.getHeaders()
            .forEach(
                (k,v) -> details.httpServletResponse.addHeader(k, v)
            );

        // TODO this assumes a json response
        details.httpServletResponse.getWriter().print(new GsonBuilder().setPrettyPrinting().create()
                                       .toJson(restResponse.getEntity()));
      } else {
        details.httpServletResponse.sendError(500, "Unhandled return type " + details.method.getReturnType() + " for method.");
      }

    } catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
      throw new GridException(e.getMessage());
    } finally {
      details.httpServletResponse.getWriter().close();
    }
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
            a -> a.getAnnotation(RestPath.class).path()
                .trim()
                .toLowerCase()
                .replaceAll("^/", ""),
            a -> a
        ));
  }

  private Map<String, Method> reflectMethods(Class<?> annotation) {
    return getRestPaths(getRestMethodsByType(annotation));
  }


}
