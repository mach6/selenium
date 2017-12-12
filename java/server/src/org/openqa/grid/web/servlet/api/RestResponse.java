package org.openqa.grid.web.servlet.api;

import com.google.common.net.MediaType;

import java.util.Collections;
import java.util.Map;

public final class RestResponse {

  private Object entity;
  private String contentType = MediaType.JSON_UTF_8.toString();
  private String encoding = "UTF-8";
  private Map<String, String> headers = Collections.emptyMap();
  private int status = 500;

  public RestResponse setEntity(Object entity) {
    this.entity = entity;
    return this;
  }

  public RestResponse setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public RestResponse setEncoding(String encoding) {
    this.encoding = encoding;
    return this;
  }

  public RestResponse addHeader(String key, String val) {
    this.headers.put(key, val);
    return this;
  }

  public RestResponse error() {
    status = 500;
    return this;
  }

  public RestResponse ok() {
    status = 200;
    return this;
  }

  public RestResponse setStatus(int status) {
    this.status = status;
    return this;
  }

  public Object getEntity() {
    return entity;
  }

  public String getContentType() {
    return contentType;
  }

  public String getEncoding() {
    return encoding;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public int getStatus() {
    return status;
  }


}
