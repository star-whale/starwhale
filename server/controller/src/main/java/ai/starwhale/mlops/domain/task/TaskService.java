/**
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

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.task.bo.ResultPathConverter;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TaskService {

    @Resource
    private TaskConvertor taskConvertor;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private ResultPathConverter resultPathConverter;

    @Resource
    private StorageAccessService storageAccessService;


    public PageInfo<TaskVO> listTasks(Long jobId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<TaskEntity> tasks = taskMapper.listTasks(jobId);

        return PageUtil.toPageInfo(tasks, taskConvertor::convert);

    }

    public List<String> offLineLogFiles(Long taskId){
        ResultPath resultPath = resultPathOfTask(taskId);
        try {
            String logDir = resultPath.logDir();
            return storageAccessService.list(logDir)
                .map(path-> trimPath(path,logDir))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("read logs path from storage failed {}",taskId,e);
            throw new SWProcessException(ErrorType.DB).tip("read log path from db failed");
        }

    }

    public String logContent(Long taskId,String logFileName){
        ResultPath resultPath = resultPathOfTask(taskId);
        String logDir = resultPath.logDir();
        try(InputStream inputStream = storageAccessService.get(
            logDir + PATH_SPLITERATOR + logFileName)) {
            return new String(inputStream.readAllBytes());
        } catch (IOException e) {
            log.error("read logs path from storage failed {}",taskId,e);
            throw new SWProcessException(ErrorType.DB).tip("read log path from db failed");
        }

    }

    private ResultPath resultPathOfTask(Long taskId) {
        TaskEntity taskById = taskMapper.findTaskById(taskId);
        ResultPath resultPath;
        try {
            resultPath = resultPathConverter.fromString(taskById.getResultPath());
        } catch (JsonProcessingException e) {
            log.error("read result path from db failed {}", taskId,e);
            throw new SWProcessException(ErrorType.DB).tip("read log path from db failed");
        }
        return resultPath;
    }

    static final String PATH_SPLITERATOR ="/";
    String trimPath(String fullPath,String dir){
        return fullPath.replace(dir,"").replace(PATH_SPLITERATOR,"");
    }

    public TaskVO findTask(Long taskId) {
        TaskEntity entity = taskMapper.findTaskById(taskId);

        return taskConvertor.convert(entity);
    }

    public Boolean addTask(Task task) {
        String uuid = task.getUuid();
        if(!StringUtils.hasText(uuid)) {
            uuid = IdUtil.simpleUUID();
            task.setUuid(uuid);
        }
        TaskEntity entity = TaskEntity.builder()
            .jobId(task.getJob().getId())
            .taskUuid(uuid)
            .taskStatus(task.getStatus())
            .build();
        return taskMapper.addTask(entity) > 0;
    }
}
