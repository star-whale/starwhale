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
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/")
public class ObjectStoreController implements ObjectStoreApi {

    private final StorageAccessService storageAccessService;

    public ObjectStoreController(StorageAccessService storageAccessService) {
        this.storageAccessService = storageAccessService;
    }

    @Override
    public void getObjectContent(String path, Long expTimeMillis, HttpServletResponse httpResponse) {
        if (expTimeMillis < System.currentTimeMillis()) {
            throw new SwValidationException(ValidSubject.OBJECT_STORE).tip("link expired");
        }
        try (InputStream fileInputStream = storageAccessService.get(path);
                ServletOutputStream outputStream = httpResponse.getOutputStream()) {
            long length = fileInputStream.transferTo(outputStream);
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"content\"");
            httpResponse.addHeader("Content-Length", String.valueOf(length));
            outputStream.flush();
        } catch (IOException e) {
            log.error("download file from storage failed {}", path, e);
            throw new SwProcessException(ErrorType.STORAGE);
        }
    }
}
