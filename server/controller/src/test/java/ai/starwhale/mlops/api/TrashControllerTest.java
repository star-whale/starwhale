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

package ai.starwhale.mlops.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;

import ai.starwhale.mlops.api.protocol.trash.TrashVo;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.trash.bo.TrashQuery;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class TrashControllerTest {

    private TrashController trashController;

    private TrashService trashService;

    @BeforeEach
    public void setUp() {
        trashService = mock(TrashService.class);

        trashController = new TrashController(trashService);
    }

    @Test
    public void testList() {
        given(trashService.listTrash(any(TrashQuery.class), any(PageParams.class), any(OrderParams.class)))
                .willReturn(new PageInfo<>(List.of(
                        TrashVo.builder().id("1").name("trash1").build(),
                        TrashVo.builder().id("2").name("trash2").build()
                )));

        var resp = trashController.listTrash("1", null, null, null, 1, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData().getList(), allOf(
                notNullValue(),
                is(iterableWithSize(2)),
                is(hasItem(hasProperty("id", is("1"))))
        ));
    }

    @Test
    public void testRecover() {
        given(trashService.recover(any(), same(1L)))
                .willReturn(true);
        given(trashService.recover(any(), same(2L)))
                .willReturn(false);

        var resp = trashController.recoverTrash("p1", 1L);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> trashController.recoverTrash("", 2L));
    }

    @Test
    public void testDelete() {
        given(trashService.deleteTrash(any(), same(1L)))
                .willReturn(true);
        given(trashService.deleteTrash(any(), same(2L)))
                .willReturn(false);

        var resp = trashController.deleteTrash("p1", 1L);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));

        assertThrows(StarwhaleApiException.class,
                () -> trashController.deleteTrash("", 2L));
    }
}
