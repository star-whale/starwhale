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
import org.springframework.stereotype.Component;

@Component
public class VersionAliasConvertor implements Convertor<Long, String> {

    @Override
    public String convert(Long order) throws ConvertException {
        if (order == null) {
            return null;
        }
        return "v" + order;
    }

    @Override
    public Long revert(String alias) throws ConvertException {
        if (StrUtil.isEmpty(alias)) {
            return null;
        }
        try {
            if (!alias.startsWith("v")) {
                throw new Exception("Alias is not start with v");
            }
            return Long.parseLong(alias.substring(1));
        } catch (Exception e) {
            throw new ConvertException("Version alias revert error: " + alias, e);
        }
    }

    public boolean isVersionAlias(String alias) {
        return alias != null
                && alias.startsWith("v")
                && StrUtil.isNumeric(alias.substring(1));
    }
}
