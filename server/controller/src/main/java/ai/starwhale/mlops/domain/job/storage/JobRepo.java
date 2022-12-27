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
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.exception.SwProcessException;
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

    private final ObjectMapper objectMapper;

    public JobRepo(DataStore store,
                   ProjectMapper projectMapper,
                   ModelVersionMapper modelMapper,
                   UserMapper userMapper, ObjectMapper objectMapper) {
        this.store = store;
        this.projectMapper = projectMapper;
        this.modelMapper = modelMapper;
        this.userMapper = userMapper;
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

    private String getTableNameByProject(Long projectId) {
        var project = projectMapper.find(projectId);
        return tableName(project.getProjectName());
    }

    private String getTableNameByUuid(String uuid) {
        var andConditions = List.of(
                (Object) TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(KeyColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.STRING, uuid)))
                    .build());

        return this.getTableNameByCondition(andConditions);
    }

    private String getTableName(Long jobId) {
        var andConditions = List.of(
                (Object) TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(LongIdColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.INT64, jobId)))
                    .build());
        return this.getTableNameByCondition(andConditions);
    }

    private String getTableNameByCondition(List<Object> conditions) {
        for (String table : tableNames()) {
            var tmp = store.query(
                    DataStoreQueryRequest.builder()
                        .tableName(table)
                        .filter(TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.AND)
                            .operands(conditions)
                            .build())
                        .build()
            ).getRecords();
            if (tmp != null && !tmp.isEmpty()) {
                return table;
            }
        }
        return null;
    }

    public void createSchema(String projectName) {
        store.update(this.tableName(projectName), tableSchemaDesc, List.of());
    }

    public int addJob(JobEntity jobEntity) {
        Map<String, Object> records = new HashMap<>();
        records.put(LongIdColumn, ColumnTypeScalar.INT64.encode(jobEntity.getId(), false));
        records.put(KeyColumn, jobEntity.getJobUuid());
        if (Objects.nonNull(jobEntity.getComment())) {
            records.put(CommentColumn, jobEntity.getComment());
        }
        if (Objects.nonNull(jobEntity.getCreatedTime())) {
            records.put(CreatedTimeColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getCreatedTime().getTime(), false));
        }
        if (Objects.nonNull(jobEntity.getModifiedTime())) {
            records.put(ModifiedTimeColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getModifiedTime().getTime(), false));
        }
        if (Objects.nonNull(jobEntity.getFinishedTime())) {
            records.put(FinishTimeColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getFinishedTime().getTime(), false));
        }
        if (Objects.nonNull(jobEntity.getDurationMs())) {
            records.put(DurationColumn,
                    ColumnTypeScalar.INT64.encode(jobEntity.getDurationMs(), false));
        }
        if (Objects.nonNull(jobEntity.getStepSpec())) {
            records.put(StepSpecColumn, jobEntity.getStepSpec());
        }
        records.put(IsDeletedColumn, "0");
        records.put(ProjectIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getProjectId(), false));
        records.put(ModelVersionIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getModelVersionId(), false));
        records.put(ModelNameColumn, jobEntity.getModelName());
        records.put(ModelVersionColumn, jobEntity.getModelVersionValue());
        records.put(RuntimeVersionIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getRuntimeVersionId(), false));
        records.put(RuntimeNameColumn, jobEntity.getRuntimeName());
        records.put(RuntimeVersionColumn, jobEntity.getRuntimeVersionValue());
        records.put(DataSetIdVersionMapColumn, convert(jobEntity.getDatasetIdVersionMap()));
        records.put(OwnerIdColumn,
                ColumnTypeScalar.INT64.encode(jobEntity.getOwnerId(), false));
        records.put(OwnerNameColumn, String.valueOf(jobEntity.getOwnerName()));
        records.put(JobStatusColumn, jobEntity.getJobStatus().name());
        records.put(JobTypeColumn, jobEntity.getType().name());
        records.put(ResultOutputPathColumn, jobEntity.getResultOutputPath());
        records.put(ResourcePoolColumn, jobEntity.getResourcePool());

        store.update(this.getTableNameByProject(jobEntity.getProjectId()), tableSchemaDesc, List.of(records));
        return 1;
    }

    public Map<String, String> convert(Map<Long, String> origin) {
        if (CollectionUtils.isEmpty(origin)) {
            return Map.of();
        }
        return origin.keySet().stream()
            .collect(Collectors.toMap(
                k -> (String) ColumnTypeScalar.INT64.encode(k, false),
                origin::get));
    }

    public List<JobEntity> listJobs(Long projectId, Long modelId) {
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

    public List<JobEntity> findJobByStatusIn(List<JobStatus> jobStatuses) {
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

        List<JobEntity> results = new ArrayList<>();
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
    private List<JobEntity> getJobEntities(ProjectEntity project, TableQueryFilter filter) {
        var results = new ArrayList<JobEntity>();

        var table = this.tableName(project.getProjectName());
        var it = new Iterator<List<JobEntity>>() {
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
            public List<JobEntity> next() {
                var records = store.query(request).getRecords();
                if (records.isEmpty()) {
                    finished = true;
                    return List.of();
                }
                // update for next request
                request.setStart(request.getStart() + records.size());

                var entities = objectMapper.convertValue(records, new TypeReference<List<JobEntity>>() {
                });
                // populate bean
                for (JobEntity job : entities) {
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

    public JobEntity findJobByUuid(String uuid) {
        var andConditions = List.of(
                (Object) TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(IsDeletedColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.INT32, 0)))
                    .build(),
                TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(KeyColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.STRING, uuid)))
                    .build());
        return this.findJobByCondition(andConditions);
    }

    public JobEntity findJobById(Long jobId) {
        var andConditions = List.of(
                (Object) TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(IsDeletedColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.INT32, 0)))
                    .build(),
                TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(LongIdColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.INT64, jobId)))
                    .build());
        return this.findJobByCondition(andConditions);
    }

    private JobEntity findJobByCondition(List<Object> conditions) {

        var projects = projectMapper.list(null, null, null);

        for (ProjectEntity project : projects) {
            var records = store.query(
                    DataStoreQueryRequest.builder()
                        .tableName(this.tableName(project.getProjectName()))
                        .filter(TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.AND)
                            .operands(conditions)
                            .build())
                        .limit(2) // no need to get more
                        .rawResult(true)
                        .build()).getRecords();
            if (records != null && !records.isEmpty()) {
                if (records.size() != 1) {
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "find multi eval jobs");
                }
                var job = objectMapper.convertValue(records.get(0), JobEntity.class);
                // populate bean
                job.setProject(project);
                job.setModelVersion(modelMapper.findByNameAndModelId(job.getModelVersionValue(), null));
                job.setOwner(userMapper.find(job.getOwnerId()));
                return job;
            }
        }
        return null;
    }

    public void updateJobStatus(Long jobId, JobStatus jobStatus) {
        var job = this.findJobById(jobId);
        if (Objects.isNull(job)) {
            return;
        }
        this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), JobStatusColumn, STRING, jobStatus.name());
    }

    public void updateJobFinishedTime(Long jobId, Date finishedTime) {
        var job = this.findJobById(jobId);
        if (Objects.isNull(job)) {
            return;
        }
        this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), FinishTimeColumn, INT64,
                (String) ColumnTypeScalar.INT64.encode(finishedTime.getTime(), false));
    }

    public int updateJobComment(Long jobId, String comment) {
        var job = this.findJobById(jobId);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), CommentColumn, STRING, comment);
    }

    public int updateJobCommentByUuid(String uuid, String comment) {
        return this.updateByUuid(this.getTableNameByUuid(uuid), uuid, CommentColumn, STRING, comment);
    }

    public int removeJob(Long jobId) {
        var job = this.findJobById(jobId);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), IsDeletedColumn, INT32, "1");
    }

    public int removeJobByUuid(String uuid) {
        return this.updateByUuid(this.getTableNameByUuid(uuid), uuid, IsDeletedColumn, INT32, "1");
    }

    public int recoverJob(Long jobId) {
        var job = this.findJobById(jobId);
        if (Objects.isNull(job)) {
            return 0;
        }
        return this.updateByUuid(this.tableName(job.getProject().getProjectName()),
                job.getJobUuid(), IsDeletedColumn, INT32, "0");
    }

    public int recoverJobByUuid(String uuid) {
        return this.updateByUuid(this.getTableNameByUuid(uuid), uuid, IsDeletedColumn, INT32, "0");
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
        return 1;
    }

}
