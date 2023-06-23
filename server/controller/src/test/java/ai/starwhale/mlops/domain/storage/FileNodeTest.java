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

package ai.starwhale.mlops.domain.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.api.protocol.storage.FileNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.activation.MimetypesFileTypeMap;
import org.junit.jupiter.api.Test;


public class FileNodeTest {

    private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

    public static FileNode parse(String path, Consumer<FileNode> filler) {
        var names = path.split("/");
        int index = 0;
        FileNode root = null;
        FileNode last = null;
        for (String name : names) {
            var node = FileNode.builder().name(name).build();
            if (index == names.length - 1) {
                node.setType(FileNode.Type.FILE);
                node.setMime(mimeTypesMap.getContentType(name));
                filler.accept(node);
            }
            if (last != null) {
                last.getFiles().add(node);
            } else {
                root = node;
            }
            // update
            last = node;
            index++;
        }
        return root;
    }

    public static void merge(List<FileNode> mainLayer, FileNode fork) {
        if (Objects.isNull(fork)) {
            return;
        }

        for (FileNode mainNode : mainLayer) {
            if (mainNode.getName().equals(fork.getName())) {
                // deal sub nodes of the fork node
                for (FileNode subForkNode : fork.getFiles()) {
                    var nextLayer = mainNode.getFiles();
                    merge(nextLayer, subForkNode);
                }
                return;
            }
        }

        // if new then only add this fork and it's children
        mainLayer.add(fork);
    }

    @Test
    public void testCompare() {
        var basePaths = List.of(
                Map.of("path", "a/b/c.yaml", "sign", "1", "desc", "SRC"),
                Map.of("path", "a/d.py", "sign", "2", "desc", "SRC"),
                Map.of("path", "a/e/f.py", "sign", "3", "desc", "SRC"),
                Map.of("path", "g.yaml", "sign", "4", "desc", "SRC"),
                Map.of("path", "x/y.pth", "sign", "5", "desc", "MODEL")
        );
        List<FileNode> baseFiles = new ArrayList<>();
        basePaths.forEach(map -> {
            var fork = parse(map.get("path"), (n) -> {
                n.setSignature(map.get("sign"));
                n.setDesc(map.get("desc"));
            });
            merge(baseFiles, fork);
        });

        var comparePaths = List.of(
                Map.of("path", "a/b/c.yaml", "sign", "1", "desc", "SRC"), // unchanged
                Map.of("path", "a/b1/c1.yaml", "sign", "12", "desc", "SRC"), // added
                Map.of("path", "a/d.py", "sign", "2", "desc", "SRC"), // unchanged
                // Map.of("path", "a/e/f.py", "sign", "3", "desc", "SRC"), deleted
                Map.of("path", "g.yaml", "sign", "4", "desc", "SRC"), // unchanged
                Map.of("path", "x/y.pth", "sign", "50", "desc", "MODEL"), //updated
                Map.of("path", "x/y1.pth", "sign", "51", "desc", "MODEL") // added
        );
        List<FileNode> compareFiles = new ArrayList<>();
        comparePaths.forEach(map -> {
            var fork = parse(map.get("path"), (n) -> {
                n.setSignature(map.get("sign"));
                n.setDesc(map.get("desc"));
            });
            merge(compareFiles, fork);
        });

        FileNode.compare(baseFiles, compareFiles);

        assertThat("comapre info", compareFiles, is(
                List.of(
                        FileNode.builder()
                                .name("a")
                                .type(FileNode.Type.DIRECTORY)
                                .files(List.of(
                                        FileNode.builder()
                                                .name("b")
                                                .type(FileNode.Type.DIRECTORY)
                                                .files(List.of(
                                                        FileNode.builder()
                                                                .name("c.yaml")
                                                                .type(FileNode.Type.FILE)
                                                                .desc("SRC")
                                                                .mime(mimeTypesMap.getContentType("c.yaml"))
                                                                .flag(FileNode.Flag.UNCHANGED)
                                                                .signature("1")
                                                                .build()
                                                ))
                                                .build(),
                                        FileNode.builder()
                                                .name("b1")
                                                .type(FileNode.Type.DIRECTORY)
                                                .flag(FileNode.Flag.ADDED)
                                                .files(List.of(
                                                        FileNode.builder()
                                                                .name("c1.yaml")
                                                                .type(FileNode.Type.FILE)
                                                                .desc("SRC")
                                                                .mime(mimeTypesMap.getContentType("c.yaml"))
                                                                .flag(FileNode.Flag.ADDED)
                                                                .signature("12")
                                                                .build()
                                                ))
                                                .build(),
                                        FileNode.builder()
                                                .name("d.py")
                                                .type(FileNode.Type.FILE)
                                                .desc("SRC")
                                                .mime(mimeTypesMap.getContentType("d.py"))
                                                .signature("2")
                                                .flag(FileNode.Flag.UNCHANGED)
                                                .build(),
                                        FileNode.builder()
                                                .name("e")
                                                .type(FileNode.Type.DIRECTORY)
                                                .flag(FileNode.Flag.DELETED)
                                                /*.files(List.of(
                                                    FileNode.builder()
                                                        .name("f.py")
                                                        .type(FileNode.Type.FILE)
                                                        .desc("SRC")
                                                        .mime(mimeTypesMap.getContentType("f.py"))
                                                        .signature("3")
                                                        .flag(FileNode.Flag.DELETED)
                                                        .build()
                                                )) */
                                                .build()
                                ))
                                .build(),
                        FileNode.builder()
                                .name("g.yaml")
                                .type(FileNode.Type.FILE)
                                .mime(mimeTypesMap.getContentType("g.yaml"))
                                .desc("SRC")
                                .signature("4")
                                .flag(FileNode.Flag.UNCHANGED)
                                .build(),
                        FileNode.builder()
                                .name("x")
                                .type(FileNode.Type.DIRECTORY)
                                .files(List.of(
                                        FileNode.builder()
                                                .name("y.pth")
                                                .type(FileNode.Type.FILE)
                                                .mime(mimeTypesMap.getContentType("y.pth"))
                                                .desc("MODEL")
                                                .signature("50")
                                                .flag(FileNode.Flag.UPDATED)
                                                .build(),
                                        FileNode.builder()
                                                .name("y1.pth")
                                                .type(FileNode.Type.FILE)
                                                .mime(mimeTypesMap.getContentType("y1.pth"))
                                                .desc("MODEL")
                                                .signature("51")
                                                .flag(FileNode.Flag.ADDED)
                                                .build()
                                ))
                                .build()
                )
        ));
    }
}
