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
