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

package ai.starwhale.mlops.configuration.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ContentCachingFilter extends OncePerRequestFilter {

    private final String apiPrefix;

    public ContentCachingFilter(@Value("${sw.controller.api-prefix}") String apiPrefix) {
        this.apiPrefix = StringUtils.trimTrailingCharacter(apiPrefix, '/') + "/datastore";
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith(apiPrefix)) {
            filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
        } else {
            filterChain.doFilter(request, response);
        }

    }

    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            InputStream requestInputStream = request.getInputStream();
            this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

    }


    public static class CachedBodyServletInputStream extends ServletInputStream {

        private InputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            try {
                return cachedBodyInputStream.available() == 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }

        @Override
        public int read() throws IOException {
            return cachedBodyInputStream.read();
        }

        @Override
        public int read(@NotNull byte[] b) throws IOException {
            return cachedBodyInputStream.read(b);
        }

        @Override
        public int read(@NotNull byte[] b, int off, int len) throws IOException {
            return cachedBodyInputStream.read(b, off, len);
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            return cachedBodyInputStream.readAllBytes();
        }

        @Override
        public byte[] readNBytes(int len) throws IOException {
            return cachedBodyInputStream.readNBytes(len);
        }

        @Override
        public int readNBytes(byte[] b, int off, int len) throws IOException {
            return cachedBodyInputStream.readNBytes(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return cachedBodyInputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return cachedBodyInputStream.available();
        }

        @Override
        public void close() throws IOException {
            cachedBodyInputStream.close();
        }

        @Override
        public void mark(int readlimit) {
            cachedBodyInputStream.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            cachedBodyInputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return cachedBodyInputStream.markSupported();
        }

        @Override
        public long transferTo(OutputStream out) throws IOException {
            return cachedBodyInputStream.transferTo(out);
        }

        public static InputStream nullInputStream() {
            return InputStream.nullInputStream();
        }
    }
}
