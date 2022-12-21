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

import static ai.starwhale.mlops.domain.job.JobSchema.BOOL;
import static ai.starwhale.mlops.domain.job.JobSchema.CommentColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.CreatedTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DataSetIdVersionMapColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DurationColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.FinishTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.INT64;
import static ai.starwhale.mlops.domain.job.JobSchema.IsDeletedColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.JobStatusColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.JobTypeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.KeyColumn;
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
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
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

@Service
public class JobRepo {
    private final DataStore store;
    private final ProjectMapper projectMapper;
    private final ModelVersionMapper modelMapper;

    private final ObjectMapper objectMapper;

    public JobRepo(DataStore store,
                   ProjectMapper projectMapper,
                   ModelVersionMapper modelMapper,
                   ObjectMapper objectMapper) {
        this.store = store;
        this.projectMapper = projectMapper;
        this.modelMapper = modelMapper;
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

    private String getTableName(Long projectId) {
        var project = projectMapper.find(projectId);
        return tableName(project.getProjectName());
    }

    private String getTableName(String jobId) {
        var andConditions = List.of(
                (Object) TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(KeyColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.STRING, jobId)))
                    .build());

        for (String table : tableNames()) {
            var tmp = store.query(
                    DataStoreQueryRequest.builder()
                        .tableName(table)
                        .filter(TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.AND)
                            .operands(andConditions)
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

    private int update(String table, String keyValue, String property, String type, String value) {
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
                    KeyColumn, keyValue,
                    ModifiedTimeColumn, String.valueOf(new Date().getTime()),
                    property, value))
        );
        // todo
        return 1;
    }

    public int addJob(JobEntity jobEntity) {
        Map<String, Object> records = new HashMap<>();
        records.put(KeyColumn, jobEntity.getId());
        if (Objects.nonNull(jobEntity.getComment())) {
            records.put(CommentColumn, jobEntity.getComment());
        }
        if (Objects.nonNull(jobEntity.getCreatedTime())) {
            records.put(CreatedTimeColumn, String.valueOf(jobEntity.getCreatedTime().getTime()));
        }
        if (Objects.nonNull(jobEntity.getModifiedTime())) {
            records.put(ModifiedTimeColumn, String.valueOf(jobEntity.getModifiedTime().getTime()));
        }
        if (Objects.nonNull(jobEntity.getFinishedTime())) {
            records.put(FinishTimeColumn, String.valueOf(jobEntity.getFinishedTime().getTime()));
        }
        if (Objects.nonNull(jobEntity.getDurationMs())) {
            records.put(DurationColumn, String.valueOf(jobEntity.getDurationMs()));
        }
        if (Objects.nonNull(jobEntity.getStepSpec())) {
            records.put(StepSpecColumn, jobEntity.getStepSpec());
        }
        records.put(IsDeletedColumn, "0");
        records.put(ProjectIdColumn, String.valueOf(jobEntity.getProjectId()));
        records.put(ModelVersionIdColumn, String.valueOf(jobEntity.getModelVersionId()));
        records.put(ModelNameColumn, jobEntity.getModelName());
        records.put(ModelVersionColumn, jobEntity.getModelVersionValue());
        records.put(RuntimeVersionIdColumn, String.valueOf(jobEntity.getRuntimeVersionId()));
        records.put(RuntimeNameColumn, jobEntity.getRuntimeName());
        records.put(RuntimeVersionColumn, jobEntity.getRuntimeVersionValue());
        records.put(DataSetIdVersionMapColumn, jobEntity.getDatasetIdVersionMap());
        records.put(OwnerIdColumn, String.valueOf(jobEntity.getOwnerId()));
        records.put(OwnerNameColumn, String.valueOf(jobEntity.getOwnerName()));
        records.put(JobStatusColumn, jobEntity.getJobStatus().name());
        records.put(JobTypeColumn, jobEntity.getType().name());
        records.put(ResultOutputPathColumn, jobEntity.getResultOutputPath());
        records.put(ResourcePoolColumn, jobEntity.getResourcePool());

        store.update(this.getTableName(jobEntity.getProjectId()), tableSchemaDesc, List.of(records));
        // todo whether to flush
        return 1;
    }

    public List<JobEntity> listJobs(Long projectId, Long modelId) {
        var andConditions = new ArrayList<>();
        andConditions.add(TableQueryFilter.builder()
                .operator(TableQueryFilter.Operator.EQUAL)
                .operands(List.of(
                    new TableQueryFilter.Column(IsDeletedColumn),
                    new TableQueryFilter.Constant(ColumnTypeScalar.BOOL, false)))
                .build());

        if (Objects.nonNull(modelId)) {
            andConditions.add(TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(ModelVersionIdColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.STRING, modelId)))
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
                                new TableQueryFilter.Constant(ColumnTypeScalar.BOOL, false)))
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
                    .rawResult(false)
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
                    job.setOwner(UserEntity.builder().id(job.getOwnerId()).userName(job.getOwnerName()).build());
                }
                return entities;
            }
        };
        while (it.hasNext()) {
            results.addAll(it.next());
        }
        return results;

    }

    public JobEntity findJobById(String jobId) {
        var andConditions = List.of(
                (Object) TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(IsDeletedColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.BOOL, false)))
                    .build(),
                TableQueryFilter.builder()
                    .operator(TableQueryFilter.Operator.EQUAL)
                    .operands(List.of(
                        new TableQueryFilter.Column(KeyColumn),
                        new TableQueryFilter.Constant(ColumnTypeScalar.STRING, jobId)))
                    .build());

        var projects = projectMapper.list(null, null, null);

        for (ProjectEntity project : projects) {
            var records = store.query(
                    DataStoreQueryRequest.builder()
                        .tableName(this.tableName(project.getProjectName()))
                        .filter(TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.AND)
                            .operands(andConditions)
                            .build())
                        .limit(2) // no need to get more
                        .rawResult(false)
                        .build()).getRecords();
            if (records != null && !records.isEmpty()) {
                if (records.size() != 1) {
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "find multi eval jobs");
                }
                var job = objectMapper.convertValue(records.get(0), JobEntity.class);
                // populate bean
                job.setProject(project);
                job.setModelVersion(modelMapper.findByNameAndModelId(job.getModelVersionValue(), null));
                job.setOwner(UserEntity.builder().id(job.getOwnerId()).userName(job.getOwnerName()).build());
                return job;
            }
        }
        return null;
    }

    public void updateJobStatus(String jobId, JobStatus jobStatus) {
        this.update(this.getTableName(jobId), jobId, JobStatusColumn, STRING, jobStatus.name());
    }

    public void updateJobFinishedTime(String jobId, Date finishedTime) {
        this.update(this.getTableName(jobId), jobId, FinishTimeColumn, INT64, String.valueOf(finishedTime.getTime()));
    }

    public int updateJobComment(String jobId, String comment) {
        return this.update(this.getTableName(jobId), jobId, CommentColumn, STRING, comment);
    }

    public int removeJob(String jobId) {
        return this.update(this.getTableName(jobId), jobId, IsDeletedColumn, BOOL, "1");

    }

    public int recoverJob(String jobId) {
        return this.update(this.getTableName(jobId), jobId, IsDeletedColumn, BOOL, "0");
    }

}
