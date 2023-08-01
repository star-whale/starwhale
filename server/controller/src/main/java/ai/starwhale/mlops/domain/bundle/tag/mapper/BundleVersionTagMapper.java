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

package ai.starwhale.mlops.domain.bundle.tag.mapper;

import ai.starwhale.mlops.domain.bundle.tag.po.BundleVersionTagEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BundleVersionTagMapper {

    String[] COLUMNS = {
            "id",
            "type",
            "bundle_id",
            "version_id",
            "tag",
            "created_time",
            "owner_id",
    };

    String TABLE = "bundle_version_tag";

    @Insert("insert into " + TABLE
            + " (type, bundle_id, version_id, tag, owner_id)"
            + " values (#{type}, #{bundleId}, #{versionId}, #{tag}, #{ownerId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(BundleVersionTagEntity entity);

    @Delete("delete from " + TABLE + " where id=#{id}")
    void deleteTagById(long id);

    @Delete("delete from " + TABLE + " where type=#{type}"
            + " and bundle_id=#{bundleId} and version_id=#{versionId} and tag=#{tag}")
    void deleteTag(String type, long bundleId, long versionId, String tag);

    @Delete("delete from " + TABLE + " where type=#{type} and bundle_id=#{bundleId}")
    void deleteAllTags(String type, long bundleId);

    @Select("select * from " + TABLE + " where type=#{type} and bundle_id=#{bundleId} and version_id=#{versionId}")
    List<BundleVersionTagEntity> listByVersionId(String type, long bundleId, long versionId);

    @Select("select * from " + TABLE + " where type=#{type} and bundle_id=#{bundleId}")
    List<BundleVersionTagEntity> listByBundleId(String type, long bundleId);

    @Select("select * from " + TABLE
            + " where type=#{type} and bundle_id=#{bundleId} and version_id in (${versionIds})")
    List<BundleVersionTagEntity> listByBundleIdVersions(String type, long bundleId, String versionIds);

    @Select("select * from " + TABLE + " where type=#{type} and bundle_id=#{bundleId} and tag=#{tag}")
    BundleVersionTagEntity findTag(String type, long bundleId, String tag);
}
