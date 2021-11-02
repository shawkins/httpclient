package io.fabric8.kubernetes.client.internal.jdkhttp;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpClient.Builder;
import io.fabric8.kubernetes.client.http.Interceptor;
import io.fabric8.kubernetes.client.http.TlsVersion;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// TODO: if there is another implementation that does not support client builder copying, then this needs to be abstracted
class JdkHttpClientBuilderImpl implements Builder {

  LinkedHashMap<String, Interceptor> interceptors = new LinkedHashMap<>();
  Duration connectTimeout;
  Duration readTimeout;
  Authenticator authenticator;
  private SSLContext sslContext;
  private Consumer<java.net.http.HttpClient.Builder> additionalConfig;
  private String proxyAuthorization;
  private InetSocketAddress proxyAddress;
  private boolean followRedirects;
  private boolean preferHttp11;
  private TlsVersion[] tlsVersions;

  @Override
  public HttpClient build() {
    java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder();
    if (connectTimeout != null) {
      builder.connectTimeout(connectTimeout);
    }
    if (authenticator != null) {
      builder.authenticator(authenticator);
    }
    if (sslContext != null) {
      builder.sslContext(sslContext);
    }
    if (followRedirects) {
      builder.followRedirects(Redirect.ALWAYS);
    }
    if (proxyAddress != null) {
      builder.proxy(ProxySelector.of(proxyAddress));
    } else {
      builder.proxy(java.net.http.HttpClient.Builder.NO_PROXY);
    }
    if (proxyAuthorization != null) {
      // TODO: just a regular inteceptor? - there is no separate support for a proxy authenticator
    }
    if (preferHttp11) {
      builder.version(Version.HTTP_1_1);
    }
    if (tlsVersions != null && tlsVersions.length > 0) {
      builder.sslParameters(new SSLParameters(null,
          Arrays.asList(tlsVersions).stream().map(TlsVersion::javaName).toArray(String[]::new)));
    }
    if (additionalConfig != null) {
      additionalConfig.accept(builder);
    }
    return new JdkHttpClientImpl(this, builder.build());
  }

  @Override
  public Builder readTimeout(long readTimeout, TimeUnit unit) {
    this.readTimeout = Duration.ofNanos(unit.toNanos(readTimeout));
    return this;
  }

  @Override
  public Builder connectTimeout(long connectTimeout, TimeUnit unit) {
    this.connectTimeout = Duration.ofNanos(unit.toNanos(connectTimeout));
    return this;
  }

  @Override
  public Builder forStreaming() {
    // nothing to do
    return this;
  }

  @Override
  public Builder writeTimeout(long timeout, TimeUnit timeoutUnit) {
    // nothing to do
    return this;
  }

  @Override
  public Builder addOrReplaceInterceptor(String name, Interceptor interceptor) {
    if (interceptor == null) {
      interceptors.remove(name);
    } else {
      interceptors.put(name, interceptor);
    }
    return this;
  }

  @Override
  public Builder authenticatorNone() {
    this.authenticator = new Authenticator() {

    };
    return this;
  }

  @Override
  public Builder sslContext(SSLContext context, TrustManager[] trustManagers) {
    this.sslContext = context;
    return this;
  }

  @Override
  public Builder followAllRedirects() {
    this.followRedirects = true;
    return this;
  }

  @Override
  public Builder proxyAddress(InetSocketAddress proxyAddress) {
    this.proxyAddress = proxyAddress;
    return this;
  }

  @Override
  public Builder proxyAuthorization(String credentials) {
    this.proxyAuthorization = credentials;
    return this;
  }

  @Override
  public Builder preferHttp11() {
    this.preferHttp11 = true;
    return this;
  }

  @Override
  public Builder tlsVersions(TlsVersion[] tlsVersions) {
    this.tlsVersions = tlsVersions;
    return this;
  }

  public Builder additionalConfig(Consumer<java.net.http.HttpClient.Builder> additionalConfig) {
    this.additionalConfig = additionalConfig;
    return this;
  }

  public Builder copy() {
    JdkHttpClientBuilderImpl copy = new JdkHttpClientBuilderImpl();
    copy.authenticator = this.authenticator;
    copy.connectTimeout = this.connectTimeout;
    copy.readTimeout = this.readTimeout;
    copy.sslContext = this.sslContext;
    copy.interceptors = new LinkedHashMap<>(this.interceptors);
    copy.followRedirects = this.followRedirects;
    copy.proxyAddress = this.proxyAddress;
    copy.proxyAuthorization = this.proxyAuthorization;
    copy.additionalConfig = this.additionalConfig;
    copy.tlsVersions = this.tlsVersions;
    copy.preferHttp11 = this.preferHttp11;
    copy.followRedirects = this.followRedirects;
    copy.additionalConfig = this.additionalConfig;
    return copy;
  }

}