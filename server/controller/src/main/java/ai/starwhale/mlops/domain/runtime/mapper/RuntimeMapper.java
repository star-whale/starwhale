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

package ai.starwhale.mlops.domain.runtime.mapper;

import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RuntimeMapper {

    List<RuntimeEntity> listRuntimes(@Param("projectId") Long projectId, @Param("namePrefix")String namePrefix);

    int addRuntime(@Param("runtime")RuntimeEntity runtime);

    int deleteRuntime(@Param("id")Long id);

    int recoverRuntime(@Param("id")Long id);

    RuntimeEntity findRuntimeById(@Param("id")Long id);

    List<RuntimeEntity> findRuntimesByIds(@Param("ids")List<Long> ids);

    RuntimeEntity findByNameForUpdate(@Param("name")String name);
    RuntimeEntity findByName(@Param("name")String name);

    RuntimeEntity findDeletedRuntimeById(@Param("id")Long id);
    List<RuntimeEntity> listDeletedRuntimes(@Param("runtimeName")String runtimeName);
}
