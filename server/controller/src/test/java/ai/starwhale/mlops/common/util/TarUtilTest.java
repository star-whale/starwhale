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

package ai.starwhale.mlops.common.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.common.TarFileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TarUtilTest {
    static File tar;

    @BeforeEach
    public void setup() throws IOException {
        tar = new File(tempDir, "test.tar");
        tar.createNewFile();
    }

    @TempDir
    static File tempDir;

    // org.apache.commons.compress.archivers.tar.TarArchiveEntry.readFileMode use “/”
    String separator = "/";
    Map<String, String> files = Map.of(
            "1.txt", "1",
            "2.txt", "12",
            "3.txt", "123",
            String.format("src%ssrc.txt", separator), "src"
    );

    @Test
    @Order(1)
    public void testArchive() throws ArchiveException, IOException {
        // create original files
        files.forEach((name, content) -> {
            try {
                var path = Path.of(tempDir.getPath(), name);
                if (name.contains(separator)) {
                    Files.createDirectories(Path.of(tempDir.getPath(),
                            name.substring(0, name.lastIndexOf(separator)))
                    );
                }
                Files.writeString(path, content, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // archive
        var fileNames = new ArrayList<>(files.keySet());
        var originLength = tar.length();
        TarFileUtil.archiveAndTransferTo(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !fileNames.isEmpty();
            }

            @Override
            public TarFileUtil.TarEntry next() {
                var fileName = fileNames.remove(0);
                try {
                    var file = Path.of(tempDir.getPath(), fileName).toFile();
                    return TarFileUtil.TarEntry.builder()
                            .inputStream(new FileInputStream(file))
                            .size(file.length())
                            .name(fileName)
                            .build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new FileOutputStream(tar));

        assertThat("archive tar", tar.length() > originLength);
    }

    @Test
    @Order(2)
    public void testOneExtractFromFile() throws IOException {
        var bytes = TarFileUtil.getContentFromTarFile(new FileInputStream(tar), "", "1.txt");
        assertThat("file bytes content", bytes != null);

        assertThat("file content", "1", is(new String(bytes)));
    }

    @Test
    @Order(3)
    public void testExtract() throws IOException, ArchiveException {
        var fileContent = new HashMap<String, String>();
        TarFileUtil.extract(new FileInputStream(tar), (name, size, in) -> {
            byte[] content = TarFileUtil.getContent(in);
            fileContent.put(name, new String(content));
        });
        assertThat("extract tar file", fileContent, is(files));
    }

}
