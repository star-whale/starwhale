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

package ai.starwhale.mlops.domain.task;

/**
 * inference task type,the complete process includes ppl and result
 */
public enum TaskType {
    UNKNOWN(-1), PPL(1), CMP(2);
    final int value;
    TaskType(int v){
        this.value = v;
    }
    public int getValue(){
        return this.value;
    }


    public static TaskType from(int v){
        for(TaskType taskType:TaskType.values()){
            if(taskType.value == v){
                return taskType;
            }
        }
        return UNKNOWN;
    }
}
