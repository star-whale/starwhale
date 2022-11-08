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

package ai.starwhale.mlops.common.util;

import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.TagAction.Action;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import cn.hutool.core.util.StrUtil;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class TagUtil {

    public static final String SEPARATOR = ",";

    public static final String ALIAS_REGEX = "^v\\d*$";

    public static final Pattern ALIAS_PATTERN = Pattern.compile(ALIAS_REGEX);

    public static String getTags(TagAction tagAction, String originTags) {
        if (!checkTags(tagAction.getTags())) {
            throw new TagException("Invalid alias or tag.");
        }
        if (tagAction.getAction() == Action.ADD) {
            return TagUtil.addTags(tagAction.getTags(), originTags);
        } else if (tagAction.getAction() == Action.REMOVE) {
            return TagUtil.removeTags(tagAction.getTags(), originTags);
        } else {
            return tagAction.getTags();
        }
    }

    public static String addTags(String newTags, String tags) {
        if (StrUtil.isEmpty(tags)) {
            return newTags;
        }
        Set<String> set = toSet(tags);
        set.addAll(StrUtil.split(newTags, SEPARATOR));
        return toString(set);
    }

    public static String removeTags(String tagsToRemove, String tags) {
        if (StrUtil.isEmpty(tags)) {
            return tags;
        }
        Set<String> set = toSet(tags);
        StrUtil.split(tagsToRemove, SEPARATOR).forEach(set::remove);
        return toString(set);
    }

    private static boolean checkTags(String tags) {
        if (StrUtil.isEmpty(tags)) {
            return true;
        }
        List<String> splits = StrUtil.split(tags, SEPARATOR);
        for (String split : splits) {
            if (ALIAS_PATTERN.matcher(split).matches()) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> toSet(String tags) {
        List<String> splits = StrUtil.split(tags, SEPARATOR);
        for (String split : splits) {
            if (ALIAS_PATTERN.matcher(split).matches()) {
                throw new TagException("Invalid alias or tag.");
            }
        }
        return new LinkedHashSet<>(StrUtil.split(tags, SEPARATOR));
    }

    private static String toString(Set<String> set) {
        return StrUtil.join(SEPARATOR, set);
    }
}
