package io.fabric8.kubernetes.client.internal.jdkhttp;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpClient.Builder;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;

import java.util.function.Consumer;

public class JdkHttpClientFactory implements HttpClient.Factory {

  @Override
  public HttpClient createHttpClient(Config config) {
    return createHttpClient(config, builder -> {});
  }

  @Override
  public Builder newBuilder() {
    return new JdkHttpClientBuilderImpl();
  }

  /**
   * Creates an HTTP client configured to access the Kubernetes API.
   * @param config Kubernetes API client config
   * @param additionalConfig a consumer that allows overriding HTTP client properties
   * @return returns an HTTP client
   */
  public io.fabric8.kubernetes.client.http.HttpClient createHttpClient(Config config, Consumer<java.net.http.HttpClient.Builder> additionalConfig) {
    JdkHttpClientBuilderImpl builderWrapper = new JdkHttpClientBuilderImpl();

    HttpClientUtils.applyCommonConfiguration(config, builderWrapper, this);

    builderWrapper.additionalConfig(additionalConfig);

    return builderWrapper.build();
  }

}
