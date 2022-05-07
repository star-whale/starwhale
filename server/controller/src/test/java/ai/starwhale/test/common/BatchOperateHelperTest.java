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

package ai.starwhale.test.common;

import ai.starwhale.mlops.common.util.BatchOperateHelper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test BatchOperateHelper
 */
public class BatchOperateHelperTest {

    static final Integer MAX_BATCH=3;
    @Test
    public void test(){
        List<Integer> batchData=List.of(1,2,3,4,5,6,7,8,9,10,11);
        BatchOperateHelper.doBatch(batchData,datas->{
            Assertions.assertTrue(datas.size()<=MAX_BATCH);
        },MAX_BATCH);
        BatchOperateHelper.doBatch(batchData,7,(datas,p)->{
            Assertions.assertTrue(datas.size()<=MAX_BATCH);
        },MAX_BATCH);
    }
}
