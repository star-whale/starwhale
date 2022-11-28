/*
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

package ai.starwhale.mlops.common;

import ai.starwhale.mlops.exception.ConvertException;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class IdConverter implements Converter<Long, String> {

    @Override
    public String convert(Long id) throws ConvertException {
        if (id == null) {
            return null;
        }
        return String.valueOf(id);
    }

    @Override
    public Long revert(String strId) throws ConvertException {
        if (!StringUtils.hasText(strId)) {
            return null;
        }
        try {
            return Long.valueOf(strId);
        } catch (NumberFormatException e) {
            throw new ConvertException("Convert ID: number format error.", e);
        }
    }

    public boolean isId(String str) {
        return StrUtil.isNumeric(str);
    }
}
