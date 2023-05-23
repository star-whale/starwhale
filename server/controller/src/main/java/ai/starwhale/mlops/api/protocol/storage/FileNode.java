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

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    /**
     * this function will update the property of flag for the compare list
     * - added: a node that exists in compare but not in base
     * - deleted: a node that not exists in compare but in base
     * - updated: a node that exists in both of base and compare but signature is different
     * - unchanged: a node that exists in both of base and compare, and signature is samp
     *
     * @param base    the base nodes list
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
