package io.fabric8.kubernetes.client.vertx;

import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class VertxHttpRequest implements HttpRequest {

    private final URI uri;
    private final RequestOptions options;
    private final Buffer body;

    public VertxHttpRequest(URI uri, RequestOptions options, Buffer body) {
        this.uri = uri;
        this.options = options;
        this.body = body;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public String method() {
        return options.getMethod().name();
    }

    @Override
    public String bodyString() {
        return body.toString();
    }

    @Override
    public List<String> headers(String key) {
        return options.getHeaders().getAll(key);
    }

    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpClient client, Class<T> clazz) {
        Future<HttpResponse<T>> fut = client.request(options).compose(request -> {
            Function<HttpClientResponse, Future<HttpResponse<T>>> responseHandler = resp -> resp
                .body(

                ).map(responseBody -> new HttpResponse<T>() {
                    @Override
                    public int code() {
                        return resp.statusCode();
                    }
                    @Override
                    public T body() {
                        if (clazz == String.class) {
                            return clazz.cast(responseBody.toString());
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }
                    @Override
                    public HttpRequest request() {
                        return VertxHttpRequest.this;
                    }
                    @Override
                    public Optional<HttpResponse<T>> previousResponse() {
                        return Optional.empty();
                    }
                    @Override
                    public List<String> headers(String key) {
                        return resp.headers().getAll(key);
                    }
                });
            if (body != null) {
                return request.send(body).compose(responseHandler);
            } else {
                return request.send().compose(responseHandler);
            }
        });
        return fut.toCompletionStage().toCompletableFuture();
    }
}
