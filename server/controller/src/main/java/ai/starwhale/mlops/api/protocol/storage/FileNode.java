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

package ai.starwhale.mlops.api.protocol.storage;

import ai.starwhale.mlops.domain.storage.MetaInfo;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.activation.MimetypesFileTypeMap;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

@Data
@Builder
public class FileNode {

    public enum Type {
        DIRECTORY("directory"), FILE("file");
        private final String name;
        Type(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    public enum Flag {
        ADDED("added"),
        UPDATED("updated"),
        DELETED("deleted"),
        UNCHANGED("unchanged");
        private final String name;
        Flag(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    private String name;

    private String signature;

    /**
     * updated/added/deleted/unchanged
     */
    @Builder.Default
    private Flag flag = Flag.UNCHANGED;

    private String mime;

    @Builder.Default
    private Type type = Type.DIRECTORY;

    /**
     * description
     */
    private String desc;

    private String size;

    @Builder.Default
    private List<FileNode> files = new ArrayList<>();

    public FileNode copyPure() {
        return FileNode.builder()
                .name(name)
                .desc(desc)
                .mime(mime)
                .type(type)
                .size(size)
                .signature(signature)
                .flag(flag)
                .build();
    }

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

    public static List<FileNode> makeTree(List<MetaInfo.Resource> resources) {
        List<FileNode> nodes = new ArrayList<>();
        for (MetaInfo.Resource fileDesc : resources) {
            // parse one path
            var fork = FileNode.parse(fileDesc.getPath(), (node) -> {
                node.setSize(FileUtil.readableFileSize(fileDesc.getSize()));
                node.setDesc(Objects.nonNull(fileDesc.getDesc()) ? fileDesc.getDesc().name() : FileDesc.UNKNOWN.name());
                node.setSignature(fileDesc.getSignature());
            });
            // merge to main nodes
            FileNode.merge(nodes, fork);
        }
        return nodes;
    }

    /**
     * this function will update the property of flag for the compare list
     *  - added: a node that exists in compare but not in base
     *  - deleted: a node that not exists in compare but in base
     *  - updated: a node that exists in both of base and compare but signature is different
     *  - unchanged: a node that exists in both of base and compare, and signature is samp
     *
     * @param base the base nodes list
     * @param compare the compare nodes list
     */
    public static void compare(List<FileNode> base, List<FileNode> compare) {
        if (CollectionUtils.isEmpty(compare)) {
            return;
        }
        var baseMap = base.stream().collect(Collectors.toMap(
                        n -> String.format("%s_%s", n.getName(), n.getType()), n -> n));
        var compareMap = compare.stream().collect(Collectors.toMap(
                        n -> String.format("%s_%s", n.getName(), n.getType()), n -> n));
        var names = Stream.concat(baseMap.keySet().stream(), compareMap.keySet().stream())
                .collect(Collectors.toSet());
        for (String name : names) {
            if (compareMap.containsKey(name)) {
                var compareNode = compareMap.get(name);
                if (baseMap.containsKey(name)) {
                    // updated or unchanged
                    var baseNode = baseMap.get(name);
                    // only update flag for file
                    if (baseNode.getType() == Type.FILE) {
                        if (Objects.equals(baseNode.getSignature(), compareNode.getSignature())) {
                            compareNode.setFlag(Flag.UNCHANGED);
                        } else {
                            compareNode.setFlag(Flag.UPDATED);
                        }
                    }
                    // go on
                    compare(baseNode.getFiles(), compareNode.getFiles());
                } else {
                    // added( TODO and update sub file flag)
                    compareNode.setFlag(Flag.ADDED);
                    updateFlag(compareNode, Flag.ADDED);
                }
            } else {
                // deleted
                // copy from base to the compare list
                var deleted = baseMap.get(name).copyPure();
                deleted.setFlag(Flag.DELETED);
                compare.add(deleted);
            }
        }
    }

    private static void updateFlag(FileNode node, Flag flag) {
        node.setFlag(flag);
        if (!CollectionUtils.isEmpty(node.getFiles())) {
            node.getFiles().forEach(subNode -> updateFlag(subNode, flag));
        }
    }
}
