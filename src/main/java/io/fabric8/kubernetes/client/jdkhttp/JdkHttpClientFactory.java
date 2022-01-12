package io.fabric8.kubernetes.client.jdkhttp;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpClient.Builder;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;

public class JdkHttpClientFactory implements HttpClient.Factory {

  @Override
  public HttpClient createHttpClient(Config config) {
    JdkHttpClientBuilderImpl builderWrapper = new JdkHttpClientBuilderImpl(this);

    HttpClientUtils.applyCommonConfiguration(config, builderWrapper, this);

    return builderWrapper.build();
  }

  @Override
  public Builder newBuilder() {
    return new JdkHttpClientBuilderImpl(this);
  }

  protected void additionalConfig(java.net.http.HttpClient.Builder builder) {

  }

  protected java.net.http.HttpClient.Builder createNewHttpClientBuilder() {
    return java.net.http.HttpClient.newBuilder();
  }

}
