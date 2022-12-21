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

package ai.starwhale.mlops.db.migration;

import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.storage.JobMapper;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import java.util.stream.Collectors;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.stereotype.Component;

@Component
public class MigrateJobToDatastore implements JavaMigration {

    private final JobMapper jobMapper;
    private final DatasetDao datasetDao;
    private final JobRepo jobRepo;
    private final ProjectManager projectManager;

    public MigrateJobToDatastore(JobMapper jobMapper,
                                 DatasetDao datasetDao,
                                 JobRepo jobRepo,
                                 ProjectManager projectManager) {
        this.jobMapper = jobMapper;
        this.datasetDao = datasetDao;
        this.jobRepo = jobRepo;
        this.projectManager = projectManager;
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("0.3.2.008");
    }

    @Override
    public String getDescription() {
        return "migrate_job_to_datastore";
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean isUndo() {
        return false;
    }

    @Override
    public boolean isBaselineMigration() {
        return false;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    public void migrate(Context context) {
        var projects = projectManager.listAllProjects();
        for (ProjectEntity project : projects) {
            jobRepo.createSchema(project.getProjectName());
            var oldJobs = jobMapper.listJobs(project.getId());
            for (JobEntity oldJob : oldJobs) {
                oldJob.setDatasetIdVersionMap(
                        datasetDao.listDatasetVersionsOfJob(oldJob.getId())
                            .stream()
                            .collect(Collectors.toMap(DatasetVersion::getId, DatasetVersion::getVersionName))
                );
                jobRepo.addJob(oldJob);
            }
        }
    }
}
