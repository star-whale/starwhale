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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import org.junit.jupiter.api.Test;

public class TagUtilTest {

    @Test
    public void testAddTags() {
        assertEquals("tag1,tag2,new", TagUtil.addTags("new", "tag1,tag2"));
        assertThrows(TagException.class, () -> TagUtil.addTags("new", "tag1,tag2,v1"));
        assertEquals("tag1,tag2,vv,new1,new2", TagUtil.addTags("new1,new2", "tag1,tag2,vv"));
        assertEquals("tag1,tag2,new1", TagUtil.addTags("new1,tag2", "tag1,tag2"));
        assertEquals("new1,new2", TagUtil.addTags("new1,new2", ""));
        assertEquals("new1,new2", TagUtil.addTags("new1,new2", null));
    }

    @Test
    public void testRemoveTags() {
        assertEquals("tag1,tag2", TagUtil.removeTags("remove", "tag1,tag2,remove"));
        assertEquals("tag1,tag2,remove1", TagUtil.removeTags("remove", "tag1,tag2,remove1"));
        assertEquals("tag1,tag3", TagUtil.removeTags("tag2", "tag1,tag2,tag3"));
        assertEquals("tag2", TagUtil.removeTags("tag1,tag3", "tag1,tag2,tag3"));
        assertEquals("", TagUtil.removeTags("tag1,tag3,tag2", "tag1,tag2,tag3"));
        assertEquals("", TagUtil.removeTags("tag2", ""));
    }

    @Test
    public void testGetTags() {
        TagAction tagAction = TagAction.of("add", "tag3");
        assertEquals("tag1,tag2,tag3", TagUtil.getTags(tagAction, "tag1,tag2"));

        tagAction = TagAction.of("remove", "tag2");
        assertEquals("tag1,tag3", TagUtil.getTags(tagAction, "tag1,tag2,tag3"));

        tagAction = TagAction.of("set", "tag2");
        assertEquals("tag2", TagUtil.getTags(tagAction, "tag1,tag2,tag3"));
    }
}
