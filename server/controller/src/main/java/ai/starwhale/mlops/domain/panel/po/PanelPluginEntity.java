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

package ai.starwhale.mlops.domain.panel.po;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PanelPluginEntity {
    private Long id;
    private String name;
    private String version;
    private String meta;
    private String storagePath;

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (PanelPluginEntity) obj;
        if (other.getId() == null || id == null) {
            return other.getName().equals(name) && other.getVersion().equals(version) && other.getMeta().equals(meta);
        }
        return Objects.equals(other.getId(), id)
            && Objects.equals(other.getName(), name)
            && Objects.equals(other.getVersion(), version)
            && Objects.equals(other.getMeta(), meta);
    }
}
