package org.openqa.grid.web.servlet.api;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

public class NonConformingRuntimeDelegate extends RuntimeDelegate {

  @Override
  public UriBuilder createUriBuilder() {
    return null;
  }

  @Override
  public Response.ResponseBuilder createResponseBuilder() {
      return new NonConformingRestResponse.NonConformingRestResponseBuilder();
  }

  @Override
  public Variant.VariantListBuilder createVariantListBuilder() {
    throw new NotImplementedException();
  }

  @Override
  public <T> T createEndpoint(Application application, Class<T> endpointType)
      throws IllegalArgumentException, UnsupportedOperationException {
    throw new NotImplementedException();
  }

  @Override
  public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
    throw new NotImplementedException();
  }
}
