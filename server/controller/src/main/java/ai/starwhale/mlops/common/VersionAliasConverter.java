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

import ai.starwhale.mlops.domain.bundle.base.HasId;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class VersionAliasConverter implements Converter<Long, String> {

    public static final String LATEST = "latest";
    public static final String BUILTIN = "built-in";

    @Override
    public String convert(Long order) throws ConvertException {
        if (order == null) {
            throw new ConvertException("Version alias covert error: order is null");
        }
        return "v" + order;
    }

    public String convert(Long order, HasId latest, HasId entity) {
        if (latest != null && Objects.equals(entity.getId(), latest.getId())) {
            return VersionAliasConverter.LATEST;
        } else {
            return convert(order);
        }
    }

    @Override
    public Long revert(String alias) throws ConvertException {
        if (!isVersionAlias(alias)) {
            throw new ConvertException("Version alias revert error: " + alias);
        }
        try {
            return Long.parseLong(alias.substring(1));
        } catch (Exception e) {
            throw new ConvertException("Version alias revert error: " + alias, e);
        }
    }

    public boolean isVersionAlias(String alias) {
        return alias != null
                && Pattern.compile("v\\d+").matcher(alias).matches();
    }

    public boolean isLatest(String alias) {
        return LATEST.equalsIgnoreCase(alias);
    }
}
