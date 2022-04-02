/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl.clsmulti;

import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.impl.clsbi.BCConfusionMetrics;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;

/**
 * Multiple Bi Classification confusion metrics to evaluate a Multiclass Classification question
 * (label,prediction) pair (A,B) will produce FN for label A and FP for label B and TN for other
 * For example if we are working on a handwriting recognition problem from 0 to 9
 * we got (label,prediction) pair (1,2) this will generate 1:FN; 2:FP; 3:TN; 4:TN; 5:TN; 6:TN; 7:TN; 8:TN; 9:TN
 */
@Slf4j
public class MBCConfusionMetrics extends Indicator<Map<String, BCConfusionMetrics>> {

    public static final String NAME = "MBCConfusionMetrics";

    final Set<String> labelSet;

    final Map<String, BCConfusionMetrics> confusionMetrics;

    final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    final Lock readLock;
    final Lock writeLock;

    /**
     * key: label
     * value: the confusion metrics for this label
     */
//    Map<String, BCConfusionMetrics> value;

    public MBCConfusionMetrics(MCConfusionMetrics mcConfusionMetrics) {
        super(NAME,null);
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        final List<MCIndicator> indicators = mcConfusionMetrics.getValue();
        labelSet = buildLabelSet(indicators);
        confusionMetrics = initConfusionMetrics(labelSet);
        for (MCIndicator mcIndicator : indicators) {
            updateMetrics(mcIndicator);
        }
        calculate();
    }

    public void calculate() {
        confusionMetrics.forEach((key, value) -> value.calculate());
        this.value = Collections.unmodifiableMap(confusionMetrics);
    }

    private void updateMetrics(MCIndicator mcIndicator) {
        //when doing TN make up for a never seen label, no feeding is allowed
        readLock.lock();
        try{
            final String label = mcIndicator.getLabel();
            final String prediction = mcIndicator.getPrediction();
            final int sampleAmount = mcIndicator.getValue().intValue();
            log.debug("updateMetrics label {} prediction {}",label,prediction);
            labelSet.forEach(lbl -> {
                final BCConfusionMetrics bcConfusionMetrics = confusionMetrics.get(lbl);
                if(mcIndicator.right()){
                    if (lbl.equals(label)){
                        bcConfusionMetrics.increaseTp(sampleAmount);
                    }else{
                        bcConfusionMetrics.increaseTn(sampleAmount);
                    }
                }else{
                    if (lbl.equals(label)) {
                        bcConfusionMetrics.increaseFn(sampleAmount);
                    } else if (lbl.equals(prediction)) {
                        bcConfusionMetrics.increaseFp(sampleAmount);
                    } else {
                        bcConfusionMetrics.increaseTn(sampleAmount);
                    }
                }
            });
        }catch (Throwable e){
            throw e;
        }finally {
            readLock.unlock();
        }

    }

    public void feed(MCIndicator freshIndicator){
        final String label = freshIndicator.getLabel();
        final String prediction = freshIndicator.getPrediction();
        checkLabel(label);
        checkLabel(prediction);
        updateMetrics(freshIndicator);
    }

    private void checkLabel(String label) {
        if(labelSet.contains(label)){
            log.debug("checking label {} exists",label);
           return;
        }
        log.debug("checking label {} NOT exists 1",label);
        synchronized (labelSet){
            if(!labelSet.contains(label)){
                log.debug("checking label {} NOT exists 2",label);

                if(confusionMetrics.isEmpty()){
                    confusionMetrics.put(label, new BCConfusionMetrics());
                }else {
                    //when doing TN make up for a never seen label, no feeding is allowed
                    writeLock.lock();
                    try{
                        final Entry<String, BCConfusionMetrics> firstMetrics = confusionMetrics.entrySet()
                            .iterator().next();
                        final BCConfusionMetrics metricsValue = firstMetrics.getValue();
                        final int totalSamples = metricsValue.getTn() + metricsValue.getTp() + metricsValue.getFp()
                            + metricsValue.getFn();
                        log.debug("make up for {} is {}",label,totalSamples);
                        confusionMetrics.put(label, new BCConfusionMetrics(0,totalSamples,0,0));
                    }catch (Throwable e){
                        throw e;
                    }finally {
                        writeLock.unlock();
                    }

                }
                labelSet.add(label);
            }
        }
    }

    private Map<String, BCConfusionMetrics> initConfusionMetrics(Set<String> labelSet) {
        Map<String, BCConfusionMetrics> confusionMetrics = new ConcurrentHashMap<>();
        labelSet.forEach(label -> confusionMetrics.put(label, new BCConfusionMetrics()));
        return confusionMetrics;
    }

    private Set<String> buildLabelSet(List<MCIndicator> indicators) {
        Set<String> labelSet = new HashSet<>();
        indicators.forEach(mcIndicator -> {
            labelSet.add(mcIndicator.getLabel());
            labelSet.add(mcIndicator.getPrediction());
        });
        return labelSet;
    }

}
