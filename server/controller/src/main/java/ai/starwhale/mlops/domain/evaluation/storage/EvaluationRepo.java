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

package ai.starwhale.mlops.domain.evaluation.storage;

import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.CommentColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.CreatedTimeColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.DataSetIdVersionMapColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.DatasetUrisColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.DatasetUrisViewColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.DevModeColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.DurationColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.FinishTimeColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.INT32;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.INT64;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.IsDeletedColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.JobStatusColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.JobTypeColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.KeyColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.LongIdColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelNameColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelUriColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelUriViewColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelVersionColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModelVersionIdColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ModifiedTimeColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.NameColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.OwnerIdColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.OwnerNameColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ProjectIdColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ResourcePoolColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.ResultOutputPathColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.RuntimeNameColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.RuntimeUriColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.RuntimeUriViewColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.RuntimeVersionColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.RuntimeVersionIdColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.STRING;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.StepSpecColumn;
import static ai.starwhale.mlops.domain.evaluation.storage.JobSchema.tableSchemaDesc;
import static ai.starwhale.mlops.domain.job.converter.UserJobConverter.FORMATTER_URI_ARTIFACT;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.Int64Value;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class EvaluationRepo {
    private final DataStore store;
    private final ProjectService projectService;
    private final ModelService modelService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public EvaluationRepo(DataStore store,
                          @Lazy ProjectService projectService,
                          @Lazy ModelService modelService,
                          @Lazy UserService userService, ObjectMapper objectMapper) {
        this.store = store;
        this.projectService = projectService;
        this.modelService = modelService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public int addJob(String table, JobFlattenEntity jobEntity) {
        store.update(
                table,
                tableSchemaDesc,
                List.of(convertToRecord(jobEntity))
        );
        store.flush();
        return 1;
    }

    @NotNull
    private Map<String, Object> convertToRecord(JobFlattenEntity entity) {
        Map<String, Object> record = new HashMap<>();
        record.put(LongIdColumn, BaseValue.encode(new Int64Value(entity.getId()), false, false));
        record.put(KeyColumn, entity.getJobUuid());
        record.put(NameColumn, entity.getName());
        if (Objects.nonNull(entity.getComment())) {
            record.put(CommentColumn, entity.getComment());
        }
        if (Objects.nonNull(entity.getCreatedTime())) {
            record.put(CreatedTimeColumn,
                    BaseValue.encode(new Int64Value(entity.getCreatedTime().getTime()), false, false));
        }
        if (Objects.nonNull(entity.getModifiedTime())) {
            record.put(ModifiedTimeColumn,
                    BaseValue.encode(new Int64Value(entity.getModifiedTime().getTime()), false, false));
        }
        if (Objects.nonNull(entity.getFinishedTime())) {
            record.put(FinishTimeColumn,
                    BaseValue.encode(new Int64Value(entity.getFinishedTime().getTime()), false, false));
        }
        if (Objects.nonNull(entity.getDurationMs())) {
            record.put(DurationColumn,
                    BaseValue.encode(new Int64Value(entity.getDurationMs()), false, false));
        }
        if (Objects.nonNull(entity.getStepSpec())) {
            record.put(StepSpecColumn, entity.getStepSpec());
        }
        record.put(DevModeColumn, entity.isDevMode() ? "1" : "0");
        record.put(IsDeletedColumn, "0");
        record.put(ProjectIdColumn,
                BaseValue.encode(new Int64Value(entity.getProjectId()), false, false));

        if (Objects.nonNull(entity.getModelVersionId())) {
            record.put(ModelVersionIdColumn,
                    BaseValue.encode(new Int64Value(entity.getModelVersionId()), false, false));
        }
        if (Objects.nonNull(entity.getModelUri())) {
            record.put(ModelUriColumn, entity.getModelUri());
        }
        if (Objects.nonNull(entity.getModelUriForView())) {
            record.put(ModelUriViewColumn, entity.getModelUriForView());
        }
        if (Objects.nonNull(entity.getModelName())) {
            record.put(ModelNameColumn, entity.getModelName());
        }
        if (Objects.nonNull(entity.getModelVersionValue())) {
            record.put(ModelVersionColumn, entity.getModelVersionValue());
        }

        if (Objects.nonNull(entity.getRuntimeVersionId())) {
            record.put(RuntimeVersionIdColumn,
                    BaseValue.encode(new Int64Value(entity.getRuntimeVersionId()), false, false));
        }
        if (Objects.nonNull(entity.getRuntimeUri())) {
            record.put(RuntimeUriColumn, entity.getRuntimeUri());
        }
        if (Objects.nonNull(entity.getRuntimeUriForView())) {
            record.put(RuntimeUriViewColumn, entity.getRuntimeUriForView());
        }
        if (Objects.nonNull(entity.getRuntimeName())) {
            record.put(RuntimeNameColumn, entity.getRuntimeName());
        }
        if (Objects.nonNull(entity.getRuntimeVersionValue())) {
            record.put(RuntimeVersionColumn, entity.getRuntimeVersionValue());
        }

        if (Objects.nonNull(entity.getDatasetIdVersionMap())) {
            record.put(DataSetIdVersionMapColumn, convertToDatastoreValue(entity.getDatasetIdVersionMap()));
        }
        if (Objects.nonNull(entity.getDatasets()) && !entity.getDatasets().isEmpty()) {
            record.put(DatasetUrisColumn, entity.getDatasets());
        }
        if (Objects.nonNull(entity.getDatasetsForView())) {
            record.put(DatasetUrisViewColumn, entity.getDatasetsForView());
        }

        record.put(OwnerIdColumn,
                BaseValue.encode(new Int64Value(entity.getOwnerId()), false, false));
        record.put(OwnerNameColumn, String.valueOf(entity.getOwnerName()));
        record.put(JobStatusColumn, entity.getJobStatus().name());
        record.put(JobTypeColumn, entity.getType().name());
        record.put(ResultOutputPathColumn, entity.getResultOutputPath());
        record.put(ResourcePoolColumn, entity.getResourcePool());
        return record;
    }

    public Map<String, String> convertToDatastoreValue(Map<Long, String> origin) {
        if (CollectionUtils.isEmpty(origin)) {
            return Map.of();
        }
        return origin.keySet().stream()
                .collect(Collectors.toMap(
                        k -> (String) BaseValue.encode(new Int64Value(k), false, false),
                        origin::get));
    }

    public void updateJobStatus(String table, Job job, JobStatus jobStatus) {
        if (!evaluationJob(job)) {
            return;
        }
        this.updateByUuid(table, job.getUuid(),
                List.of(ColumnRecord.builder().property(JobStatusColumn).type(STRING).value(jobStatus.name()).build()));
    }

    public void updateJobFinishedTime(String table, Job job, Date finishedTime, Long duration) {
        if (!evaluationJob(job)) {
            return;
        }
        this.updateByUuid(table, job.getUuid(), List.of(
                        ColumnRecord.builder()
                                .property(FinishTimeColumn)
                                .type(INT64)
                                .value((String) BaseValue.encode(new Int64Value(finishedTime.getTime()), false, false))
                                .build(),
                        ColumnRecord.builder()
                                .property(DurationColumn)
                                .type(INT64)
                                .value((String) BaseValue.encode(new Int64Value(duration), false, false))
                                .build()
                ));
    }

    public int updateJobComment(String table, Job job, String comment) {
        if (!evaluationJob(job)) {
            return 0;
        }
        return this.updateByUuid(table, job.getUuid(),
                List.of(ColumnRecord.builder().property(CommentColumn).type(STRING).value(comment).build()));
    }

    public int removeJob(String table, Job job) {
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(table, job.getUuid(),
                List.of(ColumnRecord.builder().property(IsDeletedColumn).type(INT32).value("1").build()));
    }

    public int recoverJob(String table, Job job) {
        if (!evaluationJob(job)) {
            return 0;
        }
        return this.updateByUuid(table, job.getUuid(), List.of(
                ColumnRecord.builder().property(IsDeletedColumn).type(INT32).value("0").build()));
    }

    private static boolean evaluationJob(Job job) {
        return !Objects.isNull(job) && job.getType() == JobType.EVALUATION;
    }

    @Data
    @Builder
    static class ColumnRecord {
        String property;
        String type;
        String value;
    }

    private int updateByUuid(String table, String uuid, List<ColumnRecord> columnRecords) {
        if (Objects.isNull(table)) {
            return 0;
        }
        if (CollectionUtils.isEmpty(columnRecords)) {
            return 0;
        }
        List<ColumnSchemaDesc> columns = new ArrayList<>(List.of(
                ColumnSchemaDesc.builder().name(KeyColumn).type(STRING).build(),
                ColumnSchemaDesc.builder().name(ModifiedTimeColumn).type(INT64).build()));

        Map<String, Object> values = new HashMap<>(Map.of(
                KeyColumn, uuid,
                ModifiedTimeColumn,
                BaseValue.encode(new Int64Value(new Date().getTime()), false, false)));
        // update columns
        columnRecords.forEach(record -> {
            columns.add(ColumnSchemaDesc.builder().name(record.getProperty()).type(record.getType()).build());
            values.put(record.getProperty(), record.getValue());
        });
        store.update(table,
                new TableSchemaDesc(KeyColumn, columns),
                List.of(values)
        );
        store.flush();
        return 1;
    }

    public int updateModelInfo(String table, List<String> uuids, ModelEntity newModel, ModelVersionEntity newVersion) {
        if (Objects.isNull(table) || CollectionUtils.isEmpty(uuids) || newModel == null || newVersion == null) {
            return 0;
        }

        List<ColumnSchemaDesc> columns = new ArrayList<>(List.of(
                ColumnSchemaDesc.builder().name(KeyColumn).type(STRING).build(),
                ColumnSchemaDesc.builder().name(ModelNameColumn).type(STRING).build(),
                ColumnSchemaDesc.builder().name(ModelUriColumn).type(STRING).build(),
                ColumnSchemaDesc.builder().name(ModelUriViewColumn).type(STRING).build(),
                ColumnSchemaDesc.builder().name(ModelVersionColumn).type(STRING).build(),
                ColumnSchemaDesc.builder().name(ModelVersionIdColumn).type(INT64).build()

        ));

        List<Map<String, Object>> records = new ArrayList<>();
        Project project = projectService.findProject(newModel.getProjectId());
        for (String uuid : uuids) {
            records.add(new HashMap<>(Map.of(
                    KeyColumn, uuid,
                    ModelNameColumn, newModel.getName(),
                    ModelVersionColumn, newVersion.getVersionName(),
                    ModelUriViewColumn, String.format(
                            FORMATTER_URI_ARTIFACT,
                            project.getName(),
                            "model",
                            newModel.getName(),
                            newVersion.getVersionName()
                    ),
                    ModelUriColumn, String.format(
                            FORMATTER_URI_ARTIFACT, project.getId(), "model", newModel.getId(), newVersion.getId()
                    ),
                    ModelVersionIdColumn, BaseValue.encode(new Int64Value(newVersion.getId()), false, false)
            )));
        }
        store.update(table, new TableSchemaDesc(KeyColumn, columns), records);
        store.flush();
        return uuids.size();
    }

    public void sync(String srcTable, List<String> ids, String dstTable) {

    }

}
