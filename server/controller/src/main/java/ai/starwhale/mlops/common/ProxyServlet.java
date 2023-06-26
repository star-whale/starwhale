/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.common;

import ai.starwhale.mlops.common.proxy.Service;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class ProxyServlet extends HttpServlet {
    protected HttpClient httpClient;
    protected ExecutorService exec;
    private final List<Service> services;

    public ProxyServlet(FeaturesProperties featuresProperties, List<Service> services) {
        this.services = services.stream()
                .filter(service -> {
                    if (!featuresProperties.isJobProxyEnabled()) {
                        return service instanceof WebServerInTask;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void init() throws ServletException {
        exec = Executors.newCachedThreadPool();
        httpClient = HttpClientBuilder.create().setMaxConnTotal(-1).build();
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // https://stackoverflow.com/questions/4931323/whats-the-difference-between-getrequesturi-and-getpathinfo-methods-in-httpservl
        var target = getTarget(req.getPathInfo());

        URI uri;
        try {
            uri = new URI(target);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // check if it is a websocket request
        if (req.getHeader("Upgrade") != null && req.getHeader("Upgrade").equalsIgnoreCase("websocket")) {
            workWithWebSocket(req, res, uri);
            return;
        }

        var host = new HttpHost(URIUtils.extractHost(uri));
        var path = uri.getPath();
        if (StringUtils.hasText(req.getQueryString())) {
            path = path + "?" + req.getQueryString();
        }
        var request = generateRequest(req, path);
        HttpResponse response = null;
        try {
            response = httpClient.execute(host, request);
            generateResponse(response, res);
        } catch (UnknownHostException | NoRouteToHostException | HttpHostConnectException e) {
            // return 502 if host or port is unavailable
            res.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        } catch (Exception e) {
            // return 500 if any other exception
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().println(e.getMessage());
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }

    // inspired by https://stackoverflow.com/questions/69482092
    private void workWithWebSocket(HttpServletRequest req, HttpServletResponse res, URI uri)
            throws IOException, ServletException {
        Socket sock = new Socket(uri.getHost(), uri.getPort());
        boolean closeSocket = false;
        try {
            var sockIn = sock.getInputStream();
            var sockOut = sock.getOutputStream();

            // prepare request header
            StringBuilder sb = new StringBuilder(512);

            var path = uri.getPath();
            if (StringUtils.hasText(req.getQueryString())) {
                path = path + "?" + req.getQueryString();
            }

            sb.append("GET ").append(path).append(" HTTP/1.1");
            sb.append("\r\n");
            var en = req.getHeaderNames();
            while (en.hasMoreElements()) {
                var n = en.nextElement();
                String header = req.getHeader(n);
                sb.append(n).append(": ").append(header).append("\r\n");
            }
            sb.append("\r\n");

            sockOut.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            sockOut.flush();

            StringBuilder responseBytes = new StringBuilder(512);
            int b = 0;
            while (b != -1) {
                b = sockIn.read();
                if (b != -1) {
                    responseBytes.append((char) b);
                    var len = responseBytes.length();
                    if (len >= 4
                            && responseBytes.charAt(len - 4) == '\r'
                            && responseBytes.charAt(len - 3) == '\n'
                            && responseBytes.charAt(len - 2) == '\r'
                            && responseBytes.charAt(len - 1) == '\n'
                    ) {
                        break;
                    }
                }
            }

            var rows = responseBytes.toString().split("\r\n");
            var response = rows[0];

            int idx1 = response.indexOf(' ');
            int idx2 = response.indexOf(' ', idx1 + 1);

            for (int i = 1; i < rows.length; i++) {
                String line = rows[i];
                int idx3 = line.indexOf(":");
                var k = line.substring(0, idx3);
                var headerField = line.substring(idx3 + 2);
                res.setHeader(k, headerField);
            }

            int respCode = Integer.parseInt(response.substring(idx1 + 1, idx2));
            if (respCode != HttpServletResponse.SC_SWITCHING_PROTOCOLS) {
                res.setStatus(respCode);
                res.flushBuffer();
                closeSocket = true;
            } else {
                var uh = req.upgrade(WsUpgradeHandler.class);
                uh.preInit(exec, sockIn, sockOut, sock);
            }
        } finally {
            if (closeSocket) {
                sock.close();
            }
        }
    }

    protected HttpRequest generateRequest(HttpServletRequest req, String uri) throws IOException {
        var request = new BasicHttpEntityEnclosingRequest(req.getMethod(), uri);
        request.setEntity(new InputStreamEntity(req.getInputStream(), req.getContentLength()));

        // copy headers
        var names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            var name = names.nextElement();
            if (name.equalsIgnoreCase("content-length")) {
                // use content-length generated above
                continue;
            }
            var header = req.getHeaders(name);
            while (header.hasMoreElements()) {
                var h = header.nextElement();
                request.addHeader(name, h);
            }
        }

        return request;
    }

    protected void generateResponse(HttpResponse origin, HttpServletResponse resp) throws IOException {
        var code = origin.getStatusLine().getStatusCode();
        resp.setStatus(code);
        var headers = origin.getAllHeaders();
        for (var header : headers) {
            resp.addHeader(header.getName(), header.getValue());
        }
        if (code == HttpServletResponse.SC_NOT_MODIFIED) {
            resp.setIntHeader("Content-Length", 0);
            return;
        }

        var entity = origin.getEntity();
        if (entity == null) {
            return;
        }
        if (!entity.isChunked()) {
            entity.writeTo(resp.getOutputStream());
            return;
        }
        var in = entity.getContent();
        var out = resp.getOutputStream();
        var buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }

    /**
     * get target url to proxy
     *
     * @param uri original uri
     * @return target url
     */
    public String getTarget(String uri) {
        uri = StringUtils.trimLeadingCharacter(uri, '/');
        var parts = uri.split("/", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("can not parse uri " + uri);
        }
        var prefix = parts[0];

        // find the service by prefix
        var service = services.stream().filter(s -> s.getPrefix().equals(prefix)).findFirst();
        if (service.isEmpty()) {
            throw new IllegalArgumentException("can not find service for prefix " + prefix);
        }
        return service.get().getTarget(parts[1]);
    }

    public static class WsUpgradeHandler implements HttpUpgradeHandler {
        ExecutorService exec;
        InputStream sockIn;
        OutputStream sockOut;
        Socket sock;
        Future<?> future;

        public WsUpgradeHandler() {
        }

        public void preInit(ExecutorService exec, InputStream sockIn, OutputStream sockOut, Socket sock) {
            this.exec = exec;
            this.sockIn = sockIn;
            this.sockOut = sockOut;
            this.sock = sock;
        }

        @Override
        public void init(WebConnection wc) {
            try {
                var servletIn = wc.getInputStream();
                var servletOut = wc.getOutputStream();
                future = exec.submit(() -> {
                    // read from sockIn and write to servletOut
                    try {
                        var buffer = new byte[1024];
                        int len;
                        while ((len = sockIn.read(buffer)) != -1) {
                            servletOut.write(buffer, 0, len);
                            servletOut.flush();
                        }
                    } catch (IOException ex) {
                        log.error("error in websocket handler", ex);
                    }

                    return null;
                });

                // read from servletIn and write to sockOut
                var buffer = new byte[1024];
                int len;
                while ((len = servletIn.read(buffer)) != -1) {
                    sockOut.write(buffer, 0, len);
                    sockOut.flush();
                }

                future.get();
            } catch (InterruptedException | EOFException ex) {
                log.info("websocket closed");
            } catch (Exception e) {
                log.error("error in websocket handler", e);
            } finally {
                if (future != null) {
                    future.cancel(true);
                }
            }
        }

        @Override
        public void destroy() {
            if (future != null) {
                future.cancel(true);
            }
            try {
                sock.close();
            } catch (IOException ex) {
                log.error("error closing socket", ex);
            }
        }

    }
}
