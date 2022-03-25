/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.clsbi;

import ai.starwhale.mlops.resulting.Indicator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * a Bi Classification confusion metrics:
 */
public class BCConfusionMetrics extends Indicator<BCConfusionMetrics> {

    public static final String NAME = "BCConfusionMetrics";

    /**
     * true positive amount
     */
    final AtomicInteger tp;

    /**
     * true negative amount
     */
    final AtomicInteger tn;

    /**
     * false positive amount
     */
    final AtomicInteger fp;

    /**
     * false negative amount
     */
    final AtomicInteger fn;
    double accuracy;
    double precision;
    double recall;

    public BCConfusionMetrics(){
        super(NAME,null);
        this.tp = new AtomicInteger(0);
        this.tn = new AtomicInteger(0);
        this.fp = new AtomicInteger(0);
        this.fn = new AtomicInteger(0);
        this.value = this;
    }

    public BCConfusionMetrics(int tp, int tn, int fp, int fn) {
        super(NAME,null);
        this.tp = new AtomicInteger(tp);
        this.tn = new AtomicInteger(tn);
        this.fp = new AtomicInteger(fp);
        this.fn = new AtomicInteger(fn);
        calculate();
        this.value = this;
    }

    public void calculate(){
        int divider = tp.intValue() + tn.intValue() + fp.intValue() + fn.intValue();
        if(divider == 0){
            this.accuracy = 0d;
            this.precision = 0d;
            this.recall = 0d;
            return;
        }
        this.accuracy = (tp.doubleValue() + tn.doubleValue())/ divider;
        divider = tp.intValue() + fp.intValue();
        if(divider == 0){
            this.precision = 0;
        }else{
            this.precision = tp.doubleValue() / divider;
        }
        divider = tp.intValue() + fn.intValue();
        if(divider == 0){
            this.recall = 0;
        }else{
            this.recall = tp.doubleValue() / divider;
        }
    }

    public void increaseTp(int tp) {
        this.tp.addAndGet(tp);
    }

    public void increaseTn(int tn) {
        this.tn.addAndGet(tn);
    }

    public void increaseFp(int fp) {
        this.fp.addAndGet(fp);
    }

    public void increaseFn(int fn) {
        this.fn.addAndGet(fn);
    }

    public int getTp() {
        return tp.intValue();
    }

    public int getTn() {
        return tn.intValue();
    }

    public int getFp() {
        return fp.intValue();
    }

    public int getFn() {
        return fn.intValue();
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }
}

