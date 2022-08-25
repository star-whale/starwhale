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

package ai.starwhale.mlops.domain.task.bo.ppl;

import ai.starwhale.mlops.api.protocol.report.resp.SWDSBlockVO;
import ai.starwhale.mlops.api.protocol.report.resp.TaskRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Deprecated
@AllArgsConstructor
@NoArgsConstructor
public class PPLRequest extends TaskRequest {

    /**
     * blocks may come from different SWDS
     */
    private List<SWDSBlockVO> swdsBlocks;

}
