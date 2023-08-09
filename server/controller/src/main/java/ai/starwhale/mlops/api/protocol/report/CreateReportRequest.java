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

package ai.starwhale.mlops.api.protocol.report;

import javax.validation.constraints.Size;
import lombok.Data;
import org.springframework.validation.annotation.Validated;


@Data
@Validated
public class CreateReportRequest {
    @Size(min = 1, max = 100, message = "Title length should between 1-100")
    private String title;
    @Size(max = 100, message = "Description length is too long")
    private String description;
    @Size(min = 1, message = "Content can't be null")
    private String content;
}
