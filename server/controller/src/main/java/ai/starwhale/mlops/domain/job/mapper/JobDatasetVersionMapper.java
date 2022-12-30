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

package ai.starwhale.mlops.domain.job.mapper;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface JobDatasetVersionMapper {

    @Select("select dataset_version_id from job_dataset_version_rel where job_id = #{jobId}")
    List<Long> listDatasetVersionIdsByJobId(@Param("jobId") Long jobId);

    @InsertProvider(value = JobDatasetVersionProvider.class, method = "insertSql")
    int insert(@Param("jobId") Long jodId, @Param("datasetVersionIds") Set<Long> datasetVersionIds);

    class JobDatasetVersionProvider {

        public String insertSql(@Param("jobId") Long jobId, @Param("datasetVersionIds") Set<Long> datasetVersionIds) {
            return new SQL() {
                {
                    INSERT_INTO("job_dataset_version_rel");
                    INTO_COLUMNS("job_id", "dataset_version_id");
                    for (Long datasetVersionId : datasetVersionIds) {
                        INTO_VALUES(String.valueOf(jobId), String.valueOf(datasetVersionId));
                        ADD_ROW();
                    }
                }
            }.toString();
        }
    }

}
