/**
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

package ai.starwhale.mlops.agent.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.AccessDeniedException;

/**
 * tar extractor util
 */
@Slf4j
public class TarUtil {
    public static void extractor(InputStream inputStream, String destinationDirectory) throws IOException {
        TarArchiveInputStream tarIn = null;
        try {
            tarIn = new TarArchiveInputStream(inputStream);
            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            String canonicalDestinationDirectory = new File(destinationDirectory).getCanonicalPath();
            while (tarEntry != null) {
                // Create a file for this tarEntry
                final File destPath = new File(destinationDirectory + File.separator + tarEntry.getName());
                prepareDestination(destPath, tarEntry.isDirectory());

                if (!startsWithPath(destPath.getCanonicalPath(), canonicalDestinationDirectory)) {
                    throw new IOException(
                            "Expanding " + tarEntry.getName() + " would create file outside of " + canonicalDestinationDirectory
                    );
                }

                if (!tarEntry.isDirectory()) {
                    destPath.createNewFile();
                    boolean isExecutable = (tarEntry.getMode() & 0100) > 0;
                    destPath.setExecutable(isExecutable);

                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(destPath);
                        IOUtils.copy(tarIn, out);
                    } catch (IOException e) {
                        log.error("tar extractor error:{}", e.getMessage(), e);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }
                tarEntry = tarIn.getNextTarEntry();
            }
        } finally {
            IOUtils.closeQuietly(tarIn);
        }
    }

    private static boolean startsWithPath(String destPath, String destDir) {
        if (destPath.startsWith(destDir)) {
            return true;
        } else if (destDir.length() > destPath.length()) {
            return false;
        } else {
            if (new File(destPath).exists() && !(new File(destPath.toLowerCase()).exists())) {
                return false;
            }

            return destPath.toLowerCase().startsWith(destDir.toLowerCase());
        }
    }

    private static void prepareDestination(File path, boolean directory) throws IOException {
        if (directory) {
            path.mkdirs();
        } else {
            if (!path.getParentFile().exists()) {
                path.getParentFile().mkdirs();
            }
            if (!path.getParentFile().canWrite()) {
                throw new AccessDeniedException(
                        String.format("Could not get write permissions for '%s'", path.getParentFile().getAbsolutePath()));
            }
        }
    }
}
