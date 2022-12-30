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

package ai.starwhale.mlops.domain.job.storage;

import static ai.starwhale.mlops.domain.job.JobSchema.CommentColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.CreatedTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DataSetIdVersionMapColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DurationColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.FinishTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.INT32;
import static ai.starwhale.mlops.domain.job.JobSchema.INT64;
import static ai.starwhale.mlops.domain.job.JobSchema.IsDeletedColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.JobStatusColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.JobTypeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.KeyColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.LongIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelNameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelVersionColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelVersionIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModifiedTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.OwnerIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.OwnerNameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ProjectIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ResourcePoolColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ResultOutputPathColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeNameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeVersionColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeVersionIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.STRING;
import static ai.starwhale.mlops.domain.job.JobSchema.StepSpecColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.tableSchemaDesc;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnTypeScalar;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreQueryRequest;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class JobRepo {
    private final DataStore store;
    private final ProjectMapper projectMapper;
    private final ModelVersionMapper modelMapper;
    private final UserMapper userMapper;
    private final JobMapper mainStore;
    private final ObjectMapper objectMapper;

    public JobRepo(DataStore store,
                   ProjectMapper projectMapper,
                   ModelVersionMapper modelMapper,
                   UserMapper userMapper, JobMapper mainStore, ObjectMapper objectMapper) {
        this.store = store;
        this.projectMapper = projectMapper;
        this.modelMapper = modelMapper;
        this.userMapper = userMapper;
        this.mainStore = mainStore;
        this.objectMapper = objectMapper;
    }

    private List<String> tableNames() {
        var projects = projectMapper.list(null, null, null);
        return projects.stream()
            .map(ProjectEntity::getProjectName)
            .map(this::tableName)
            .collect(Collectors.toList());
    }

    private String tableName(String projectName) {
        /*
         * such as project/starwhale/eval/summary
         */
        String tableNameFormat = "project/%s/eval/summary";
        return String.format(tableNameFormat, projectName);
    }

    public int addJob(JobFlattenEntity jobEntity) {
        store.update(
                this.tableName(jobEntity.getProject().getProjectName()),
                tableSchemaDesc,
                List.of(convertToRecord(jobEntity))
        );
        store.flush();
        return 1;
    }

    @NotNull
    private Map<String, Object> convertToRecord(JobFlattenEntity jobEntity) {
        Map<String, Object> record = new HashMap<>();
        record.put(LongIdColumn, ColumnTypeScalar.INT64.encode(jobEntity.getId(), false));
        record.put(KeyColumn, jobEntity.getJobUuid());
        if (Objects.nonNull(jobEntity.getComment())) {
            record.put(CommentColumn, jobEntity.getComment());
        }
        if (Objects.nonNull(jobEntity.getCreatedTime())) {
            record.put(CreatedTimeColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getCreatedTime().getTime(), false));
        }
        if (Objects.nonNull(jobEntity.getModifiedTime())) {
            record.put(ModifiedTimeColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getModifiedTime().getTime(), false));
        }
        if (Objects.nonNull(jobEntity.getFinishedTime())) {
            record.put(FinishTimeColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getFinishedTime().getTime(), false));
        }
        if (Objects.nonNull(jobEntity.getDurationMs())) {
            record.put(DurationColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getDurationMs(), false));
        }
        if (Objects.nonNull(jobEntity.getStepSpec())) {
            record.put(StepSpecColumn, jobEntity.getStepSpec());
        }
        record.put(IsDeletedColumn, "0");
        record.put(ProjectIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getProjectId(), false));
        record.put(ModelVersionIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getModelVersionId(), false));
        record.put(ModelNameColumn, jobEntity.getModelName());
        record.put(ModelVersionColumn, jobEntity.getModelVersionValue());
        record.put(RuntimeVersionIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getRuntimeVersionId(), false));
        record.put(RuntimeNameColumn, jobEntity.getRuntimeName());
        record.put(RuntimeVersionColumn, jobEntity.getRuntimeVersionValue());
        record.put(DataSetIdVersionMapColumn, convertToDatastoreValue(jobEntity.getDatasetIdVersionMap()));
        record.put(OwnerIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getOwnerId(), false));
        record.put(OwnerNameColumn, String.valueOf(jobEntity.getOwnerName()));
        record.put(JobStatusColumn, jobEntity.getJobStatus().name());
        record.put(JobTypeColumn, jobEntity.getType().name());
        record.put(ResultOutputPathColumn, jobEntity.getResultOutputPath());
        record.put(ResourcePoolColumn, jobEntity.getResourcePool());
        return record;
    }

    public Map<String, String> convertToDatastoreValue(Map<Long, String> origin) {
        if (CollectionUtils.isEmpty(origin)) {
            return Map.of();
        }
        return origin.keySet().stream()
            .collect(Collectors.toMap(
                k -> (String) ColumnTypeScalar.INT64.encode(k, false),
                origin::get));
    }

    public List<JobFlattenEntity> listJobs(Long projectId, Long modelId) {
        var andConditions = new ArrayList<>();
        andConditions.add(TableQueryFilter.builder()
                .operator(TableQueryFilter.Operator.EQUAL)
                .operands(List.of(
                    new TableQueryFilter.Column(IsDeletedColumn),
                    new TableQueryFilter.Constant(ColumnTypeScalar.INT32, 0)))
                .build());

        if (Objects.nonNull(modelId)) {
            andConditions.add(TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(ModelVersionIdColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.INT64, modelId)))
                    .build());
        }
        var filter = TableQueryFilter.builder()
                .operator(TableQueryFilter.Operator.AND)
                .operands(andConditions)
                .build();

        var project = projectMapper.find(projectId);

        return getJobEntities(project, filter);
    }

    public List<JobFlattenEntity> findJobByStatusIn(List<JobStatus> jobStatuses) {
        var filter = TableQueryFilter.builder()
                .operator(TableQueryFilter.Operator.AND)
                .operands(
                    List.of(
                        TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.OR)
                            .operands(jobStatuses.stream()
                                .map(status -> (Object) TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(
                                        new TableQueryFilter.Column(JobStatusColumn),
                                        new TableQueryFilter.Constant(ColumnTypeScalar.STRING, status.name()))
                                    ).build())
                                .collect(Collectors.toList()))
                            .build(),
                        TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(
                                new TableQueryFilter.Column(IsDeletedColumn),
                                new TableQueryFilter.Constant(ColumnTypeScalar.INT32, 0)))
                            .build()
                    ))
                .build();

        List<JobFlattenEntity> results = new ArrayList<>();
        // find all projects
        var projects = projectMapper.list(null, null, null);
        for (ProjectEntity project : projects) {
            results.addAll(this.getJobEntities(project, filter));
        }
        return results;
    }

    public List<ObjectCountEntity> countJob(List<Long> projectIds) {
        List<ObjectCountEntity> results = new ArrayList<>();
        var filter = TableQueryFilter.builder()
                .operator(TableQueryFilter.Operator.EQUAL)
                .operands(List.of(
                    new TableQueryFilter.Column(IsDeletedColumn),
                    new TableQueryFilter.Constant(ColumnTypeScalar.INT32, 0)))
                .build();
        for (Long projectId : projectIds) {
            var project = projectMapper.find(projectId);
            if (project == null) {
                continue;
            }
            results.add(ObjectCountEntity.builder()
                    .projectId(projectId)
                    .count(this.getJobEntities(project, filter).size())
                    .build());
        }
        return results;
    }

    @NotNull
    private List<JobFlattenEntity> getJobEntities(ProjectEntity project, TableQueryFilter filter) {
        var results = new ArrayList<JobFlattenEntity>();

        var table = this.tableName(project.getProjectName());
        var it = new Iterator<List<JobFlattenEntity>>() {
            boolean finished = false;
            final DataStoreQueryRequest request = DataStoreQueryRequest.builder()
                    .tableName(table)
                    .filter(filter)
                    .start(0)
                    .orderBy(List.of(OrderByDesc.builder().columnName(CreatedTimeColumn).descending(true).build()))
                    .rawResult(true)
                    .build();
            @Override
            public boolean hasNext() {
                return !finished;
            }

            @Override
            public List<JobFlattenEntity> next() {
                var records = store.query(request).getRecords();
                if (records.isEmpty()) {
                    finished = true;
                    return List.of();
                }
                // update for next request
                request.setStart(request.getStart() + records.size());

                var entities = objectMapper.convertValue(records, new TypeReference<List<JobFlattenEntity>>() {
                });
                // populate bean
                for (JobFlattenEntity job : entities) {
                    job.setProject(project);
                    job.setModelVersion(modelMapper.findByNameAndModelId(job.getModelVersionValue(), null));
                    job.setOwner(userMapper.find(job.getOwnerId()));
                }
                return entities;
            }
        };
        while (it.hasNext()) {
            results.addAll(it.next());
        }
        return results;

    }


    public void updateJobStatus(Long jobId, JobStatus jobStatus) {
        var job = mainStore.findJobById(jobId);
        if (Objects.isNull(job)) {
            return;
        }
        this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), JobStatusColumn, STRING, jobStatus.name());
    }

    public void updateJobFinishedTime(Long jobId, Date finishedTime) {
        var job = mainStore.findJobById(jobId);
        if (Objects.isNull(job)) {
            return;
        }
        this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), FinishTimeColumn, INT64,
                (String) ColumnTypeScalar.INT64.encode(finishedTime.getTime(), false));
    }

    public int updateJobComment(Long jobId, String comment) {
        var job = mainStore.findJobById(jobId);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), CommentColumn, STRING, comment);
    }

    public int updateJobCommentByUuid(String uuid, String comment) {
        var job = mainStore.findJobByUuid(uuid);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                uuid, CommentColumn, STRING, comment);
    }

    public int removeJob(Long jobId) {
        var job = mainStore.findJobById(jobId);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), IsDeletedColumn, INT32, "1");
    }

    public int removeJobByUuid(String uuid) {
        var job = mainStore.findJobByUuid(uuid);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                uuid, IsDeletedColumn, INT32, "1");
    }

    public int recoverJob(Long jobId) {
        var job = mainStore.findJobById(jobId);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), IsDeletedColumn, INT32, "0");
    }

    public int recoverJobByUuid(String uuid) {
        var job = mainStore.findJobByUuid(uuid);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                uuid, IsDeletedColumn, INT32, "0");
    }

    private int updateByUuid(String table, String uuid, String property, String type, String value) {
        if (Objects.isNull(table)) {
            return 0;
        }
        store.update(table,
                new TableSchemaDesc(KeyColumn, List.of(
                    ColumnSchemaDesc.builder().name(KeyColumn).type(STRING).build(),
                    ColumnSchemaDesc.builder().name(ModifiedTimeColumn).type(INT64).build(),
                    ColumnSchemaDesc.builder().name(property).type(type).build()
                )),
                List.of(Map.of(
                    KeyColumn, uuid,
                    ModifiedTimeColumn, ColumnTypeScalar.INT64.encode(new Date().getTime(), false),
                    property, value))
        );
        store.flush();
        return 1;
    }

}
