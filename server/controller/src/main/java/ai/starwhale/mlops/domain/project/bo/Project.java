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

package ai.starwhale.mlops.domain.project.bo;

import ai.starwhale.mlops.domain.user.bo.User;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Project {

    private Long id;

    private String name;

    private String description;

    private String overview;

    private Privacy privacy;

    private User owner;

    private boolean isDefault;

    private boolean isDeleted;

    private Date createdTime;

    public Integer getDeleteInt() {
        return isDeleted ? 1 : 0;
    }

    public static Project system() {
        return Project.builder()
                .id(0L)
                .name("SYSTEM")
                .privacy(Privacy.PUBLIC)
                .isDeleted(false)
                .isDefault(false)
                .build();
    }

    public enum Privacy {
        PUBLIC(1),
        PRIVATE(0);

        private final int value;

        Privacy(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Privacy fromName(String name) {
            try {
                return Privacy.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Privacy parse error. ", e);
                return PRIVATE;
            }
        }

        public static Privacy fromValue(Integer value) {
            if (value == null) {
                return PRIVATE;
            }
            for (Privacy privacy : Privacy.values()) {
                if (privacy.getValue() == value) {
                    return privacy;
                }
            }
            return PRIVATE;
        }
    }
}
