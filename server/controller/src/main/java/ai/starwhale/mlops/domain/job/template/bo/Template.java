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

package ai.starwhale.mlops.domain.job.template.bo;

import ai.starwhale.mlops.domain.job.template.po.TemplateEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    private Long id;
    private String name;
    private Long jobId;
    private Long projectId;
    private Long ownerId;

    public static TemplateEntity toEntity(Template template) {
        if  (template == null) {
            return null;
        }
        return TemplateEntity.builder()
                .id(template.getId())
                .name(template.getName())
                .jobId(template.getJobId())
                .projectId(template.getProjectId())
                .ownerId(template.getOwnerId())
                .build();
    }

    public static Template fromEntity(TemplateEntity templateEntity) {
        if (null == templateEntity) {
            return null;
        }
        return new Template(
                templateEntity.getId(),
                templateEntity.getName(),
                templateEntity.getJobId(),
                templateEntity.getProjectId(),
                templateEntity.getOwnerId()
        );
    }
}
