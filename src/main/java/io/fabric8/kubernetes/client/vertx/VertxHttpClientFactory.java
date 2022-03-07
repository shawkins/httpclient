package io.fabric8.kubernetes.client.vertx;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.Interceptor;
import io.fabric8.kubernetes.client.http.TlsVersion;
import io.fabric8.kubernetes.client.http.WebSocket;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocketConnectOptions;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VertxHttpClientFactory implements io.fabric8.kubernetes.client.http.HttpClient.Factory {
    
    private Vertx vertx;

    public VertxHttpClientFactory() {
        this.vertx = Vertx.vertx();
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient  createHttpClient(Config config) {
        io.fabric8.kubernetes.client.http.HttpClient.Builder builder = newBuilder();
        return builder.build();
    }
    
    private class VertxHttpClient implements io.fabric8.kubernetes.client.http.HttpClient {
        
        private HttpClient client;

        private VertxHttpClient(HttpClientOptions options) {
            client = vertx.createHttpClient(options);
        }
        
        @Override
        public void close() {
            client.close();
        }

        @Override
        public DerivedClientBuilder newBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest httpRequest, Class<T> clazz) throws IOException {
            try {
                return sendAsync(httpRequest, clazz).get(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
            } catch (TimeoutException e) {
                throw new IOException(e);
            }
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest httpRequest, Class<T> clazz) {
            VertxHttpRequest vertxHttpRequest = (VertxHttpRequest) httpRequest;
            return vertxHttpRequest.sendAsync(client, clazz);
        }

        @Override
        public WebSocket.Builder newWebSocketBuilder() {
            return new WebSocket.Builder() {
                WebSocketConnectOptions options = new WebSocketConnectOptions();
                @Override
                public CompletableFuture<io.fabric8.kubernetes.client.http.WebSocket> buildAsync(WebSocket.Listener listener) {
                    Future<io.fabric8.kubernetes.client.http.WebSocket> map = client
                        .webSocket(options)
                        .map(ws -> {
                            VertxWebSocket ret = new VertxWebSocket(ws, listener);
                            ret.init();
                            return ret;
                        });
                    return map.toCompletionStage().toCompletableFuture();
                }
                @Override
                public WebSocket.Builder subprotocol(String protocol) {
                    options.setSubProtocols(Collections.singletonList(protocol));
                    return this;
                }
                @Override
                public WebSocket.Builder header(String name, String value) {
                    options.putHeader(name, value);
                    return this;
                }
                @Override
                public WebSocket.Builder setHeader(String k, String v) {
                    options.putHeader(k, v);
                    return this;
                }
                @Override
                public WebSocket.Builder uri(URI uri) {
                    options.setAbsoluteURI(uri.toString());
                    return this;
                }
            };
        }

        @Override
        public HttpRequest.Builder newHttpRequestBuilder() {
            return new HttpRequest.Builder() {

                private URI uri;
                private RequestOptions options = new RequestOptions();
                private Buffer body;

                @Override
                public HttpRequest build() {
                    return new VertxHttpRequest(uri, new RequestOptions(options).setAbsoluteURI(uri.toString()), body);
                }
                @Override
                public HttpRequest.Builder uri(String uri) {
                    return uri(URI.create(uri));
                }
                @Override
                public HttpRequest.Builder url(URL url) {
                    return uri(url.toString());
                }
                @Override
                public HttpRequest.Builder uri(URI uri) {
                    this.uri = uri;
                    return this;
                }
                @Override
                public HttpRequest.Builder post(String contentType, byte[] bytes) {
                    options.setMethod(HttpMethod.POST);
                    options.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                    body = Buffer.buffer(bytes);
                    return this;
                }
                @Override
                public HttpRequest.Builder post(String contentType, InputStream stream, long length) {
                    throw new UnsupportedOperationException();
                }
                @Override
                public HttpRequest.Builder method(String method, String contentType, String s) {
                    options.setMethod(HttpMethod.valueOf(method));
                    options.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                    body = Buffer.buffer(s);
                    return this;
                }
                @Override
                public HttpRequest.Builder header(String k, String v) {
                    options.putHeader(k, v);
                    return this;
                }
                @Override
                public HttpRequest.Builder setHeader(String k, String v) {
                    options.putHeader(k, v);
                    return this;
                }
                @Override
                public HttpRequest.Builder expectContinue() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Override
    public io.fabric8.kubernetes.client.http.HttpClient.Builder newBuilder() {
        HttpClientOptions options = new HttpClientOptions();
        return new io.fabric8.kubernetes.client.http.HttpClient.Builder() {
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient build() {
                return new VertxHttpClient(options);
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder readTimeout(long l, TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder connectTimeout(long l, TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder forStreaming() {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder writeTimeout(long l, TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder addOrReplaceInterceptor(String s, Interceptor interceptor) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder authenticatorNone() {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder sslContext(SSLContext sslContext, TrustManager[] trustManagers) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder followAllRedirects() {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder proxyAddress(InetSocketAddress inetSocketAddress) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder proxyAuthorization(String s) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder tlsVersions(TlsVersion[] tlsVersions) {
                throw new UnsupportedOperationException();
            }
            @Override
            public io.fabric8.kubernetes.client.http.HttpClient.Builder preferHttp11() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
