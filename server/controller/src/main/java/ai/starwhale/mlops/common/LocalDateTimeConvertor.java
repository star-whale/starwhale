/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.common;

import ai.starwhale.mlops.exception.ConvertException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class LocalDateTimeConvertor implements Convertor<LocalDateTime, String>{

    @Override
    public String convert(LocalDateTime localDateTime) throws ConvertException {
        return localDateTime.toString();
    }

    @Override
    public LocalDateTime revert(String s) throws ConvertException {
        return LocalDateTime.parse(s);
    }
}
