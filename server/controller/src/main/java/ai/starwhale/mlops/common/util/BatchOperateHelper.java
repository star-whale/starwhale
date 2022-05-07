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

package ai.starwhale.mlops.common.util;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class BatchOperateHelper {

    /**
     * split one giant batch to small batches
     */
    public static <T,U> void doBatch(Collection<T> batchObject,U param, BiConsumer<Collection<T>,U> batchOperate,final Integer maxBatchSize){
        if(batchObject.size()>maxBatchSize){
            batchObject.parallelStream()
                .collect(Collectors.groupingBy(s -> ThreadLocalRandom.current().nextInt(1,3)>1))
                .forEach((k,smallerBatch)->{
                    doBatch(smallerBatch,param,batchOperate,maxBatchSize);
                });
        }else{
            batchOperate.accept(batchObject,param);
        }

    }

    /**
     * split one giant batch to small batches
     */
    public static <T> void doBatch(Collection<T> batchObject, Consumer<Collection<T>> batchOperate,final Integer maxBatchSize){
        if(batchObject.size()>maxBatchSize){
            batchObject.parallelStream()
                .collect(Collectors.groupingBy(s -> ThreadLocalRandom.current().nextInt(1,3)>1))
                .forEach((k,smallerBatch)->{
                    doBatch(smallerBatch,batchOperate,maxBatchSize);
                });
        }else{
            batchOperate.accept(batchObject);
        }

    }

}
