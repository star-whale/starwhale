/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * the storage unit of one data set
 * {
 *    "id": 1,          //第n条记录
 *    "batch": 50,      //用户自定义的batch数量，即会将多少样本达成一个整体写入到swds_bin中，默认是1
 *    "data": {
 *        "file": xx,     //数据所在的文件名
 *        "offset": 0,    //数据偏移位置
 *        "size": 0,      //字节数量
 *    },
 *    "label": {
 *        "file": xx,     //数据所在的文件名
 *        "offset": 0,    //数据偏移位置
 *        "size": 0,      //字节数量
 *    }
 * }
 */
@Data
@Builder
public class SWDSBlock {

    /**
     * the offset to the original SWDS
     */
    Long id;

    /**
     * how many batches does this block contains
     */
    @JsonProperty("batch")
    int batchAmount;

    /**
     * location of labels in this block
     */
    @JsonProperty("label")
    SWDSDataLocation locationLabel;

    /**
     * location of inputs in this block
     */
    @JsonProperty("data")
    SWDSDataLocation locationInput;

}
