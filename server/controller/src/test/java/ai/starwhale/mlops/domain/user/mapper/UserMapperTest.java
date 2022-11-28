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

package ai.starwhale.mlops.domain.user.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class UserMapperTest extends MySqlContainerHolder {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testAddAndGet() {
        String userName = UUID.randomUUID().toString();
        UserEntity user = UserEntity.builder().userEnabled(0).userName(userName).userPwdSalt("x").userPwd("up").build();
        userMapper.insert(user);
        Assertions.assertEquals(user, userMapper.find(user.getId()));
        Assertions.assertEquals(user, userMapper.findByName(userName));
    }

    @Test
    public void testListUsers() {
        UserEntity user1 = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        UserEntity user2 = UserEntity.builder().userEnabled(0).userName("un13").userPwdSalt("x").userPwd("up").build();
        UserEntity user3 = UserEntity.builder().userEnabled(0).userName("un23").userPwdSalt("x").userPwd("up").build();
        UserEntity user4 = UserEntity.builder().userEnabled(0).userName("xy65").userPwdSalt("x").userPwd("up").build();
        userMapper.insert(user1);
        userMapper.insert(user2);
        userMapper.insert(user3);
        userMapper.insert(user4);
        List<UserEntity> userEntities = userMapper.list("un", null);
        Collections.sort(userEntities, Comparator.comparing(UserEntity::getId));
        Assertions.assertIterableEquals(List.of(user1, user2, user3), userEntities);
        userEntities = userMapper.list("un1", null);
        Collections.sort(userEntities, Comparator.comparing(UserEntity::getId));
        Assertions.assertIterableEquals(List.of(user1, user2), userEntities);
        userEntities = userMapper.list("un2", null);
        Collections.sort(userEntities, Comparator.comparing(UserEntity::getId));
        Assertions.assertIterableEquals(List.of(user3), userEntities);
        userEntities = userMapper.list("xy65", null);
        Collections.sort(userEntities, Comparator.comparing(UserEntity::getId));
        Assertions.assertIterableEquals(List.of(user4), userEntities);
    }

    @Test
    public void testChangePassword() {
        UserEntity user1 = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.insert(user1);
        user1.setUserPwd("pds");
        user1.setUserPwdSalt("slt");
        userMapper.updatePassword(user1.getId(), user1.getUserPwd(), user1.getUserPwdSalt());
        Assertions.assertEquals(user1, userMapper.find(user1.getId()));
    }

    @Test
    public void testEnableUser() {
        UserEntity user1 = UserEntity.builder().userEnabled(0).userName("un12").userPwdSalt("x").userPwd("up").build();
        userMapper.insert(user1);
        user1.setUserEnabled(1);
        userMapper.updateEnabled(user1.getId(), user1.getUserEnabled());
        Assertions.assertEquals(user1, userMapper.find(user1.getId()));
        user1.setUserEnabled(0);
        userMapper.updateEnabled(user1.getId(), user1.getUserEnabled());
        Assertions.assertEquals(user1, userMapper.find(user1.getId()));
    }

}
