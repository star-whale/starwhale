/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.common.util;

import ai.starwhale.mlops.domain.swds.SWDataSetSlice;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ArrayHelper {

    public static final ArrayHelper INSTANCE = new ArrayHelper();

    /**
     * slice  [0,total] into multiple segments
     * (8,3) -> (0 3 6 7); (8,4) -> (0 2 4 6 7); (3,4) -> (0 1 2); slicePoints.size() == sliceAmount +
     * 1  || slicePoints.size() == total + 1
     */
    public List<Integer> slicePoints(final Integer total, final Integer sliceAmount) {
        if (sliceAmount <= 0) {
            log.error("invalid slice amount expected greater than 0");
            throw new SWValidationException(ValidSubject.JOB);
        }
        //if sliceAmount > total ,sliceSize is 1 and eventually we got (total + 1) slicePoints instead of (sliceAmount + 1)
        final int sliceSizeRound = total / sliceAmount;
        final int sliceSize = total % sliceAmount == 0 ? sliceSizeRound : sliceSizeRound + 1;
        int slicePoint = 0;
        List<Integer> slicePoints = new ArrayList<>(getInitialCapacity(sliceAmount + 1));
        do {
            slicePoints.add(slicePoint);
            slicePoint += sliceSize;
        } while (slicePoint < total - 1);
        slicePoints.add(total - 1);
        return slicePoints;
    }

    public List<Segment> sliceSegments(final Integer total, final Integer sliceAmount){
        final List<Integer> slicePoints = slicePoints(total, sliceAmount);
        List<Segment> sliceSegments = new ArrayList<>(getInitialCapacity(sliceAmount));
        for (int i = 0; i < sliceAmount; i++) {
            boolean lastSlice = i == sliceAmount - 1;
            sliceSegments.add(new Segment(slicePoints.get(i),lastSlice ? slicePoints.get(i + 1): slicePoints.get(i + 1) - 1));
        }
        return sliceSegments;

    }

    /**
     *
     * @param expectedSize expected array size
     * @return pre-allocated array size
     */
    public int getInitialCapacity(Integer expectedSize) {
        double deLoadFactor = 1 / 0.75;
        return Double.valueOf(Math.ceil((expectedSize) * deLoadFactor)).intValue() + 1;
    }

    @Data
    @AllArgsConstructor
    public static class Segment{
        int startInclusive;
        int endInclusive;
    }

}
