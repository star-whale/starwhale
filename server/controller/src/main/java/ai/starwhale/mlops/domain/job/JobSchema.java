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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import java.util.List;

public interface JobSchema {
    String STRING = "STRING";
    String LIST = "LIST";
    String OBJECT = "OBJECT";
    String MAP = "MAP";
    String INT32 = "INT32";
    String INT64 = "INT64";
    String BOOL = "BOOL";

    String KeyColumn = "id";
    String LongIdColumn = "sys/id";
    String NameColumn = "sys/name";
    String ProjectIdColumn = "sys/project_id";
    String ModelVersionIdColumn = "sys/model_version_id";
    String ModelNameColumn = "sys/model_name";
    String ModelVersionColumn = "sys/model_version";
    String RuntimeVersionIdColumn = "sys/runtime_version_id";
    String RuntimeNameColumn = "sys/runtime_name";
    String RuntimeVersionColumn = "sys/runtime_version";
    String DataSetIdVersionMapColumn = "sys/_dataset_id_version_map";
    String DataSetsColumn = "sys/datasets";
    String OwnerIdColumn = "sys/owner_id";
    String OwnerNameColumn = "sys/owner_name";
    String FinishTimeColumn = "sys/finished_time";
    String CreatedTimeColumn = "sys/created_time";
    String ModifiedTimeColumn = "sys/modified_time";
    String DurationColumn = "sys/duration_ms";
    String JobStatusColumn = "sys/job_status";
    String JobTypeColumn = "sys/job_type";
    String ResultOutputPathColumn = "sys/result_output_path";
    String StepSpecColumn = "sys/step_spec";
    String ResourcePoolColumn = "sys/resource_pool";
    String CommentColumn = "sys/job_comment";
    String DevModeColumn = "sys/dev_mode";
    String DevWayColumn = "sys/dev_way";
    String IsDeletedColumn = "sys/_is_deleted";
    TableSchemaDesc tableSchemaDesc = new TableSchemaDesc(KeyColumn, List.of(
            ColumnSchemaDesc.builder().name(KeyColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(NameColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(LongIdColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(ProjectIdColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(ModelVersionIdColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(ModelNameColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(ModelVersionColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(RuntimeVersionIdColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(RuntimeNameColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(RuntimeVersionColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(DataSetIdVersionMapColumn).type(MAP)
                .keyType(ColumnSchemaDesc.builder().type(INT64).build())
                .valueType(ColumnSchemaDesc.builder().type(STRING).build())
                .build(),
            ColumnSchemaDesc.builder().name(DataSetsColumn).type(LIST)
                .elementType(ColumnSchemaDesc.builder()
                        .type(OBJECT)
                        .pythonType("object")
                        .attributes(List.of(
                                ColumnSchemaDesc.builder().name("version_id").type(INT64).build(),
                                ColumnSchemaDesc.builder().name("version_tag").type(STRING).build(),
                                ColumnSchemaDesc.builder().name("dataset_id").type(INT64).build(),
                                ColumnSchemaDesc.builder().name("project_id").type(INT64).build(),
                                ColumnSchemaDesc.builder().name("dataset_name").type(STRING).build(),
                                ColumnSchemaDesc.builder().name("dataset_version").type(STRING).build(),
                                ColumnSchemaDesc.builder().name("index_table").type(STRING).build()
                        ))
                        .build())
                .build(),
            ColumnSchemaDesc.builder().name(OwnerIdColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(OwnerNameColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(CreatedTimeColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(ModifiedTimeColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(FinishTimeColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(DurationColumn).type(INT64).build(),
            ColumnSchemaDesc.builder().name(JobStatusColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(JobTypeColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(ResultOutputPathColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(StepSpecColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(ResourcePoolColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(CommentColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(DevWayColumn).type(STRING).build(),
            ColumnSchemaDesc.builder().name(DevModeColumn).type(INT32).build(),
            ColumnSchemaDesc.builder().name(IsDeletedColumn).type(INT32).build()
    ));
}
