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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class TarFileUtil {

    /**
     * Find the contents of a specified file in a tar file.
     */
    public static byte[] getContentFromTarFile(InputStream tarFileInputStream, String targetFilePath,
            String targetFileName) {
        ArchiveInputStream archiveInputStream = null;
        try {
            archiveInputStream = getArchiveInputStream(tarFileInputStream);
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                if (entry.getSize() <= 0) {
                    continue;
                }
                if (!StringUtils.isEmpty(targetFilePath) && !entry.getName().startsWith(targetFilePath)) {
                    continue;
                }
                if (!StringUtils.isEmpty(targetFileName) && !entry.getName().endsWith(targetFileName)) {
                    continue;
                }
                return getContent(archiveInputStream);
            }

        } catch (Exception e) {
            log.error("get tar file failed!", e);
        } finally {
            if (null != archiveInputStream) {
                try {
                    archiveInputStream.close();
                } catch (IOException e) {
                    log.error("file close error!", e);
                }
            }
        }

        return null;
    }

    private static ArchiveInputStream getArchiveInputStream(InputStream in) throws ArchiveException {
        return new ArchiveStreamFactory()
                .createArchiveInputStream("tar", new BufferedInputStream(in));
    }

    public static byte[] getContent(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int len = is.read(buffer);

                if (len == -1) {
                    break;
                }

                baos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();

    }

}
