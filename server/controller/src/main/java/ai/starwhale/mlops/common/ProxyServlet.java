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

import static ai.starwhale.mlops.domain.job.ModelServingService.MODEL_SERVICE_PREFIX;

import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProxyServlet extends HttpServlet {
    protected ModelServingMapper modelServingMapper;
    protected HttpClient httpClient;

    public ProxyServlet(ModelServingMapper modelServingMapper) {
        this.modelServingMapper = modelServingMapper;
    }

    @Override
    public void init() {
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

        var host = new HttpHost(URIUtils.extractHost(uri));
        var path = uri.getPath();
        if (StringUtils.hasText(req.getQueryString())) {
            path = path + "?" + req.getQueryString();
        }
        var request = generateRequest(req, path);
        try {
            var response = httpClient.execute(host, request);
            generateResponse(response, res);
        } catch (UnknownHostException | HttpHostConnectException e) {
            // return 502 if host or port is unavailable
            res.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
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
        var entity = origin.getEntity();
        if (entity == null) {
            return;
        }
        entity.writeTo(resp.getOutputStream());
    }

    /**
     * get target url to proxy, only support model serving service for now
     *
     * @param uri original uri
     * @return target url
     */
    public String getTarget(String uri) {
        uri = StringUtils.trimLeadingCharacter(uri, '/');
        var parts = uri.split("/", 3);
        if (parts.length < 2) {
            throw new IllegalArgumentException("can not parse uri " + uri);
        }
        if (!parts[0].equals(MODEL_SERVICE_PREFIX)) {
            throw new IllegalArgumentException("can not recognize prefix " + parts[0]);
        }
        var id = Long.parseLong(parts[1]);

        // TODO add cache
        if (modelServingMapper.find(id) == null) {
            throw new IllegalArgumentException("can not find model serving entry " + parts[1]);
        }

        var svc = ModelServingService.getServiceName(id);
        var handler = "";
        if (parts.length == 3) {
            handler = parts[2];
        }
        return String.format("http://%s/%s", svc, handler);
    }
}
