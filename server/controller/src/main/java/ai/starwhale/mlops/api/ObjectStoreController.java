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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple2;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "Object Store")
@RequestMapping("/")
@ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "fs")
public class ObjectStoreController {

    private final StorageAccessService storageAccessService;

    public ObjectStoreController(StorageAccessService storageAccessService) {
        this.storageAccessService = storageAccessService;
    }

    public static final String URI_PREFIX = "obj-store";

    @Operation(summary = "Get the content of an object or a file")
    @GetMapping(value = "/" + URI_PREFIX + "/**", produces = MediaType.APPLICATION_JSON_VALUE)
    void getObjectContent(
            @RequestHeader(name = "Range", required = false) String range,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpResponse
    ) {
        Tuple2<Long, String> info = extractInfoFromUri(httpServletRequest);
        Long expTimeMillis = info._1();
        String path = info._2();
        if (expTimeMillis < System.currentTimeMillis()) {
            throw new SwValidationException(ValidSubject.OBJECT_STORE, "link expired");
        }
        Long start = 0L;
        Long end = 0L;
        boolean withRange;
        if (null != range) {
            String[] split = range.split("="); //"start-end"
            if (split.length < 2) {
                withRange = false;
            } else {
                start = Long.valueOf(split[1].split("-")[0]);
                end = Long.valueOf(split[1].split("-")[1]);
                withRange = true;
            }
        } else {
            withRange = false;
        }

        try (InputStream fileInputStream = withRange ? storageAccessService.get(path, start, end - start + 1) :
                storageAccessService.get(path);
                ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"content\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download file from storage failed {}", path, e);
            throw new SwProcessException(ErrorType.STORAGE, "download file from storage failed", e);
        }

    }

    @Operation(summary = "Put the content of an object or a file")
    @PutMapping(value = "/" + URI_PREFIX + "/**", produces = MediaType.APPLICATION_JSON_VALUE)
    void modifyObjectContent(HttpServletRequest httpServletRequest) throws IOException {
        Tuple2<Long, String> info = extractInfoFromUri(httpServletRequest);
        Long expTimeMillis = info._1();
        String path = info._2();
        if (expTimeMillis < System.currentTimeMillis()) {
            throw new SwValidationException(ValidSubject.OBJECT_STORE, "link expired");
        }
        storageAccessService.put(path, httpServletRequest.getInputStream());
    }

    private Tuple2<Long, String> extractInfoFromUri(HttpServletRequest request) {
        String subpath = request.getRequestURI()
                .split(request.getContextPath() + "/" + URI_PREFIX + "/")[1];
        if (!subpath.contains("/")) {
            throw new SwValidationException(ValidSubject.OBJECT_STORE, "link expired");
        }
        int slashLastIndex = subpath.lastIndexOf("/");
        Long expmilis = Long.valueOf(subpath.substring(slashLastIndex + 1));
        return new Tuple2<>(expmilis, subpath.substring(0, slashLastIndex));
    }
}
