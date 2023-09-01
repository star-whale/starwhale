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

package ai.starwhale.mlops.domain.evaluation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.storage.UriAccessor;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class EvaluationFileStorageTest {
    private StoragePathCoordinator storagePathCoordinator;
    private StorageAccessService storageAccessService;
    private ProjectService projectService;
    private UriAccessor uriAccessor;
    private EvaluationFileStorage fileStorage;

    @BeforeEach
    public void setup() {
        uriAccessor = mock(UriAccessor.class);
        storageAccessService = mock(StorageAccessService.class);
        storagePathCoordinator = mock(StoragePathCoordinator.class);
        projectService = mock(ProjectService.class);
        fileStorage = new EvaluationFileStorage(
                storagePathCoordinator,
                storageAccessService,
                projectService,
                uriAccessor
        );
    }

    @Test
    public void testLinksOf() {
        given(uriAccessor.linkOf(eq("a"), anyLong()))
                .willReturn("link1");
        given(uriAccessor.linkOf(eq("b"), anyLong()))
                .willReturn("link2");
        given(uriAccessor.linkOf(eq("x"), anyLong()))
                .willThrow(SwValidationException.class);

        Assertions.assertEquals(
                Map.of("a", "link1", "b", "link2", "x", ""),
                fileStorage.signLinks(Set.of("a", "b", "x"), 1L)
        );
    }

    @Test
    public void testDataOf() {
        given(uriAccessor.dataOf(anyString(), any(), any())).willReturn(new byte[1]);

        var res = fileStorage.dataOf("a", 1L, 1L);
        assertThat(res, notNullValue());
    }

}
