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

package ai.starwhale.mlops.domain.ft;

import ai.starwhale.mlops.domain.evaluation.storage.EvaluationRepo;
import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.JobUpdateWatcher;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class FineTuneEvaluationUpdateWatcher implements JobUpdateWatcher {

    private static final String TABLE_NAME_FORMAT = "project/%s/ftspace/%s/eval/summary";

    private final EvaluationRepo evaluationRepo;

    public FineTuneEvaluationUpdateWatcher(EvaluationRepo evaluationRepo) {
        this.evaluationRepo = evaluationRepo;
    }

    @Override
    public boolean match(BizType bizType, JobType jobType) {
        return bizType == BizType.FINE_TUNE && jobType == JobType.EVALUATION;
    }

    @Override
    public void onCreate(JobFlattenEntity job) {
        var res = evaluationRepo.addJob(
                String.format(TABLE_NAME_FORMAT, job.getProject().getId(), job.getBizId()), job) > 0;
        if (res) {
            throw new SwProcessException(ErrorType.DATASTORE, "Sync fine-tune evaluation job failed");
        }
    }

    @Override
    public void onUpdateStatus(Job job, JobStatus jobStatus) {
        evaluationRepo.updateJobStatus(
                String.format(TABLE_NAME_FORMAT, job.getProject().getId(), job.getBizId()), job, jobStatus);
    }

    @Override
    public void onUpdateFinishTime(Job job, Date finishedTime, Long duration) {
        evaluationRepo.updateJobFinishedTime(
                String.format(TABLE_NAME_FORMAT, job.getProject().getId(), job.getBizId()),
                job,
                finishedTime,
                duration
        );
    }
}
