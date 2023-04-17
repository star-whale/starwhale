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

package ai.starwhale.mlops.datastore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes the schema of a column or an attribute of an object, including its name and type.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ColumnHintsDesc {

    private Set<String> typeHints;

    /**
     * This field represents the type of elements in the list. It is only used when type is LIST. The name field of
     * elementType is never used.
     */
    private ColumnHintsDesc elementHints;

    /**
     * This field represents the type of keys in the map. It is only used when type is MAP. The name field of
     * keyType is never used.
     */
    private ColumnHintsDesc keyHints;

    /**
     * This field represents the type of values in map. It is only used when type is MAP. The name field of
     * valueType is never used.
     */
    private ColumnHintsDesc valueHints;

    /**
     * This field describes the attributes of object type.
     */
    private Map<String, ColumnHintsDesc> attributesHints;

    public void merge(ColumnHintsDesc other) {
        if (other == null) {
            return;
        }
        if (other.typeHints != null) {
            this.typeHints.addAll(other.typeHints);
        }
        if (other.elementHints != null) {
            if (this.elementHints == null) {
                this.elementHints = new ColumnHintsDesc();
            }
            this.elementHints.merge(other.elementHints);
        }
        if (other.keyHints != null) {
            if (this.keyHints == null) {
                this.keyHints = new ColumnHintsDesc();
            }
            this.keyHints.merge(other.keyHints);
        }
        if (other.valueHints != null) {
            if (this.valueHints == null) {
                this.valueHints = new ColumnHintsDesc();
            }
            this.valueHints.merge(other.valueHints);
        }
        if (other.attributesHints != null) {
            if (this.attributesHints == null) {
                this.attributesHints = new HashMap<>();
            }
            other.attributesHints.forEach((k, v) -> {
                if (this.attributesHints.containsKey(k)) {
                    this.attributesHints.get(k).merge(v);
                } else {
                    this.attributesHints.put(k, v);
                }
            });
        }
    }
}
