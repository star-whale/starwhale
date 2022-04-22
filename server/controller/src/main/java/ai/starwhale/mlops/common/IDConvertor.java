/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.common;

import ai.starwhale.mlops.exception.ConvertException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IDConvertor implements Convertor<Long, String>{

    @Override
    public String convert(Long id) throws ConvertException {
        if(id == null) {
            return null;
        }
        return String.valueOf(id);
    }

    @Override
    public Long revert(String strId) throws ConvertException {
        if(strId == null) {
            return null;
        }
        try {
            return Long.valueOf(strId);
        } catch(NumberFormatException e) {
            throw new ConvertException("Convert ID: number format error.", e);
        }
    }
}
