/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl.clsmulti;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * hold the json line of result file
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceResult {

    Integer index;
    @JsonProperty("result")
    List<Integer> inferenceResult;
    @JsonProperty("label")
    List<Integer> labels;
    Integer batch;

    public List<MCIndicator> toMCIndicators(){
        List<MCIndicator> indicators = new LinkedList<>();
        for (int i=0;i<inferenceResult.size() && i<labels.size();i++){
            indicators.add(new MCIndicator(labels.get(i).toString(),inferenceResult.get(i).toString()));
        }
        return indicators;
    }

}
