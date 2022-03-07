import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.WebSocket;
import io.fabric8.kubernetes.client.vertx.VertxHttpClientFactory;
import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class HttpClientTest {

    private volatile Handler<HttpServerRequest> reqHandler;
    private volatile Handler<ServerWebSocket> wsHandler;
    private Vertx vertx;
    private HttpServer server;

    @Before
    public void setUp(TestContext ctx) {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer().requestHandler(req -> {
            Handler<HttpServerRequest> handler = reqHandler;
            if (handler != null) {
                handler.handle(req);
            } else {
                req.response().setStatusCode(500).end();
            }
        }).webSocketHandler(ws -> {
            Handler<ServerWebSocket> handler = wsHandler;
            if (handler != null) {
                handler.handle(ws);
            } else {
                ws.close();
            }
        });
        server
            .listen(8080, "localhost")
            .onComplete(ctx.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext ctx) throws Exception {
        vertx.close(ctx.asyncAssertSuccess());
    }

    @Test
    public void testHttpRequest() throws IOException {
        reqHandler = req -> {
            req.response().end("Hello World");
        };
        HttpClient.Factory factory = new VertxHttpClientFactory();
        HttpClient.Builder builder = factory.newBuilder();
        HttpClient client = builder.build();
        HttpRequest request = client.newHttpRequestBuilder().uri("http://localhost:8080").build();
        HttpResponse<String> resp = client.send(request, String.class);
        assertEquals(200, resp.code());
        assertEquals("Hello World", resp.body());
    }

    @Test
    public void testWebSocket(TestContext ctx) {
        Async async = ctx.async();
        wsHandler = ws -> {
            ws.handler(buff -> {
                // Echo
                ws.write(buff);
                ws.close();
            });
        };
        HttpClient.Factory factory = new VertxHttpClientFactory();
        HttpClient.Builder builder = factory.newBuilder();
        HttpClient client = builder.build();
        WebSocket.Builder wsBuilder = client.newWebSocketBuilder();
        wsBuilder.uri(URI.create("http://localhost:8080/websocket")).buildAsync(new WebSocket.Listener() {
            private Buffer received = Buffer.buffer();
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.send(Buffer.buffer("Hello World").getByteBuf().nioBuffer());
            }
            @Override
            public void onMessage(WebSocket webSocket, ByteBuffer bytes) {
                received.appendBuffer(Buffer.buffer(Unpooled.copiedBuffer(bytes)));
            }
            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                ctx.assertEquals(1000, code);
                ctx.assertEquals("Hello World", received.toString());
                async.complete();
            }
        });
    }
}
