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
import ai.starwhale.mlops.common.TagAction.ACTION;
import cn.hutool.core.util.StrUtil;
import java.util.LinkedHashSet;
import java.util.Set;

public class TagUtil {

    public static final String SEPARATOR = ",";

    public static String getTags(TagAction tagAction, String originTags) {
        if(tagAction.getAction() == ACTION.ADD) {
            return TagUtil.addTags(tagAction.getTags(), originTags);
        } else if (tagAction.getAction() == ACTION.REMOVE) {
            return TagUtil.removeTags(tagAction.getTags(), originTags);
        } else {
            return tagAction.getTags();
        }
    }

    public static String addTags(String newTags, String tags) {
        if(StrUtil.isEmpty(tags)) {
            return newTags;
        }
        Set<String> set = toSet(tags);
        set.addAll(StrUtil.split(newTags, SEPARATOR));
        return toString(set);
    }

    public static String removeTags(String tagsToRemove, String tags) {
        if(StrUtil.isEmpty(tags)) {
            return tags;
        }
        Set<String> set = toSet(tags);
        StrUtil.split(tagsToRemove, SEPARATOR).forEach(set::remove);
        return toString(set);
    }

    private static Set<String> toSet(String tags) {
        return new LinkedHashSet<>(StrUtil.split(tags, SEPARATOR));
    }

    private static String toString(Set<String> set) {
        return StrUtil.join(SEPARATOR, set);
    }

}
