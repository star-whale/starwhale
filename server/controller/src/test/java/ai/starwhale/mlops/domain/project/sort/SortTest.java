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

package ai.starwhale.mlops.domain.project.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;

import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectVisitedMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SortTest {

    private RecentlyVisitedSort recentlyVisitedSort;
    private LatestSort latestSort;
    private OldestSort oldestSort;

    private ProjectMapper projectMapper;
    private ProjectVisitedMapper projectVisitedMapper;

    @BeforeEach
    public void setUp() {
        ProjectEntity p1 = ProjectEntity.builder().id(1L).build();
        ProjectEntity p2 = ProjectEntity.builder().id(2L).build();
        ProjectEntity p3 = ProjectEntity.builder().id(3L).build();
        ProjectEntity p4 = ProjectEntity.builder().id(4L).build();
        projectMapper = Mockito.mock(ProjectMapper.class);
        Mockito.when(projectMapper.listAll(anyString(), same("id desc")))
                .thenReturn(List.of(p4, p3, p2, p1));

        Mockito.when(projectMapper.listAll(anyString(), same("id asc")))
                .thenReturn(List.of(p1, p2, p3, p4));

        Mockito.when(projectMapper.listOfUser(anyString(), anyLong(), same("id desc")))
                .thenReturn(List.of(p4, p3, p1));

        Mockito.when(projectMapper.listOfUser(anyString(), anyLong(), same("id asc")))
                .thenReturn(List.of(p1, p2));

        projectVisitedMapper = Mockito.mock(ProjectVisitedMapper.class);
        Mockito.when(projectVisitedMapper.listVisitedProjects(same(1L)))
                .thenReturn(List.of(1L, 3L));

        Mockito.when(projectVisitedMapper.listVisitedProjects(same(2L)))
                .thenReturn(List.of(4L, 2L));

        recentlyVisitedSort = new RecentlyVisitedSort(projectMapper, projectVisitedMapper);
        latestSort = new LatestSort(projectMapper);
        oldestSort = new OldestSort(projectMapper);

    }

    @Test
    public void testList() {
        User user = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();
        var res = oldestSort.list("", user, true);
        assertResult(res, List.of(1L, 2L, 3L, 4L));

        res = oldestSort.list("", user, false);
        assertResult(res, List.of(1L, 2L));

        res = latestSort.list("", user, true);
        assertResult(res, List.of(4L, 3L, 2L, 1L));

        res = latestSort.list("", user, false);
        assertResult(res, List.of(4L, 3L, 1L));

        res = recentlyVisitedSort.list("", user, true);
        assertResult(res, List.of(1L, 3L, 4L, 2L));

        res = recentlyVisitedSort.list("", user, false);
        assertResult(res, List.of(1L, 3L, 4L));

        res = recentlyVisitedSort.list("", user2, true);
        assertResult(res, List.of(4L, 2L, 3L, 1L));

        res = recentlyVisitedSort.list("", user2, false);
        assertResult(res, List.of(4L, 3L, 1L));
    }

    private void assertResult(List<ProjectEntity> result, List<Long> expectIds) {
        assertEquals(result.size(), expectIds.size());
        for (int i = 0; i < result.size(); i++) {
            assertEquals(expectIds.get(i), result.get(i).getId());
        }
    }

}
