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

package ai.starwhale.mlops.domain.report;

import ai.starwhale.mlops.api.protocol.report.ReportVo;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import ai.starwhale.mlops.domain.user.UserService;
import org.springframework.stereotype.Component;

@Component
public class ReportConverter {
    private final UserService userService;

    public ReportConverter(UserService userService) {
        this.userService = userService;
    }

    public ReportVo convert(ReportEntity entity) {
        return ReportVo.builder()
                .uuid(entity.getUuid())
                .name(entity.getTitle())
                .content(entity.getContent())
                .shared(entity.getShared())
                .owner(userService.findUserById(entity.getOwnerId()))
                .createdTime(entity.getCreatedTime().getTime())
                .modifiedTime(entity.getModifiedTime().getTime())
                .build();
    }
}
