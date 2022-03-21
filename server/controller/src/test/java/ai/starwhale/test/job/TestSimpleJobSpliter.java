/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.job;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.job.SimpleJobSpliter;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSimpleJobSpliter {

    SimpleJobSpliter simpleJobSpliter = new SimpleJobSpliter();

    public void testOneDataSet(final int deviceAmount,final int dataSetSize){
        Job job = mockOneDSJob(deviceAmount, dataSetSize);
        List<TaskTrigger> taskTriggers = simpleJobSpliter.split(job);
        Assertions.assertEquals(taskTriggers.size(),deviceAmount);
        final int sliceSizeRound = dataSetSize / deviceAmount;
        int sliceSize = dataSetSize % deviceAmount == 0? sliceSizeRound : sliceSizeRound + 1;
        int dataRemaining = dataSetSize;
        for(int i=0;i<deviceAmount;i++){
            System.out.println(dataRemaining);
            TaskTrigger taskTrigger = taskTriggers.get(i);
            Assertions.assertEquals(taskTrigger.getSwDataSetSlice().size(),1);
            if(i == deviceAmount -1){
                Assertions.assertEquals(taskTrigger.getSwDataSetSlice().get(0).getEnd() - taskTrigger.getSwDataSetSlice().get(0).getStart() ,dataRemaining -1 );
            }else {
                Assertions.assertEquals(taskTrigger.getSwDataSetSlice().get(0).getEnd() - taskTrigger.getSwDataSetSlice().get(0).getStart()  ,sliceSize - 1);
            }
            dataRemaining -= sliceSize;
        }
    }
    @Test
    public void testSplit(){
        testOneDataSet(3,2000);
        testOneDataSet(4,2000);
    }

    private Job mockOneDSJob(int deviceAmount,int dataSetSize) {
        return Job.builder()
            .id(1L)
            .jobRuntime(JobRuntime.builder().deviceAmount(deviceAmount).deviceClass(Clazz.CPU).build())
            .status(JobStatus.CREATED)
            .swDataSets(Arrays.asList(SWDataSet.builder().size(dataSetSize).build()))
            .swmp(SWModelPackage.builder().build())
            .build();
    }


}
