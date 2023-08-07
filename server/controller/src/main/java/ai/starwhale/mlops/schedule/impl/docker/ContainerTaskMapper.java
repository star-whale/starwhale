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

package ai.starwhale.mlops.schedule.impl.docker;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerTaskMapper {

    static final String CONTAINER_NAME_PREFIX="starwhale-task-";
    static final Pattern CONTAINER_NAME_PATTERN=Pattern.compile("/"+CONTAINER_NAME_PREFIX + "([1-9][0-9]?)");

    public String containerNameOfTask(Task task){
        return String.format("%s%d", CONTAINER_NAME_PREFIX, task.getId());
    }

    public Long taskIfOfContainer(String name){
        Matcher matcher = CONTAINER_NAME_PATTERN.matcher(name);
        if(matcher.matches()){
            return Long.valueOf(matcher.group(1));
        }
        throw new SwValidationException(ValidSubject.TASK,"container name can't be resolve to task id "+name);
    }

}
