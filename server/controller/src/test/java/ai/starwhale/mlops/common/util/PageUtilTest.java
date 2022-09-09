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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

import com.github.pagehelper.Page;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PageUtilTest {

    @Test
    public void testToPageInfo() {
        List<Integer> page = new Page<>(1, 5);
        page.add(123);
        page.add(456);

        var pageInfo = PageUtil.toPageInfo(page, String::valueOf);
        assertThat(pageInfo, allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(1))),
                is(hasProperty("pageSize", is(5))),
                is(hasProperty("size", is(2))),
                is(hasProperty("list", allOf(
                        iterableWithSize(2),
                        hasItem("123"),
                        hasItem("456")
                )))
        ));

        page = List.of(123, 456);
        pageInfo = PageUtil.toPageInfo(page, String::valueOf);
        assertThat(pageInfo, allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(1))),
                is(hasProperty("pageSize", is(2))),
                is(hasProperty("size", is(2))),
                is(hasProperty("list", allOf(
                        iterableWithSize(2),
                        hasItem("123"),
                        hasItem("456")
                )))
        ));
    }

}
