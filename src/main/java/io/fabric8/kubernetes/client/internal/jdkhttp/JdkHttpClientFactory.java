package io.fabric8.kubernetes.client.internal.jdkhttp;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpClient.Builder;

public class JdkHttpClientFactory implements HttpClient.Factory {

  @Override
  public HttpClient createHttpClient(Config config) {
    return null;
  }

  @Override
  public Builder newBuilder() {
    return new JdkHttpClientImpl.BuilderImpl();
  }

}
