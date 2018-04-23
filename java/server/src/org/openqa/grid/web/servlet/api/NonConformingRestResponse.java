package org.openqa.grid.web.servlet.api;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

public final class NonConformingRestResponse extends Response {

  private Object entity;
  private MultivaluedMap<String, Object> metaData;
  private int status;
  URI location;

  @Override
  public Object getEntity() {
    return entity;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public MultivaluedMap<String, Object> getMetadata() {
    return metaData;
  }

  public static class NonConformingRestResponseBuilder extends ResponseBuilder {
    NonConformingRestResponse restResponse = new NonConformingRestResponse();

    @Override
    public Response build() {
      return restResponse;
    }

    @Override
    public ResponseBuilder clone() {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder status(int status) {
      restResponse.status = status;
      return this;
    }

    @Override
    public ResponseBuilder entity(Object entity) {
      restResponse.entity = entity;
      return this;
    }

    @Override
    public ResponseBuilder type(MediaType type) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder type(String type) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder variant(Variant variant) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder variants(List<Variant> variants) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder language(String language) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder language(Locale language) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder location(URI location) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder contentLocation(URI location) {
      restResponse.location = location;
      return this;
    }

    @Override
    public ResponseBuilder tag(EntityTag tag) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder tag(String tag) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder lastModified(Date lastModified) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder cacheControl(CacheControl cacheControl) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder expires(Date expires) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder header(String name, Object value) {
      throw new NotImplementedException();
    }

    @Override
    public ResponseBuilder cookie(NewCookie... cookies) {
      throw new NotImplementedException();
    }
  }

}
