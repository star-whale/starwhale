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
