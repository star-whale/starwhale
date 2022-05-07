/**
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

import com.github.pagehelper.Page;
import com.github.pagehelper.Page.Function;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.stream.Collectors;

public class PageUtil {

    public static <T, E> PageInfo<T> toPageInfo(List<E> list, Function<E, T> function) {
        if (list instanceof Page) {
            return ((Page<E>)list).toPageInfo(function);
        } else {
            return PageInfo.of(list.stream().map(function::apply).collect(Collectors.toList()));
        }
    }

}
