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

package ai.starwhale.mlops.domain.dataset.mapper;

import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionViewEntity;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.Objects;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface DatasetVersionMapper {

    String COLUMNS = "id, version_order, dataset_id, owner_id,"
            + " version_name, version_tag, version_meta, files_uploaded, storage_path,"
            + " status, created_time, modified_time, size, index_table, shared";

    String VERSION_VIEW_COLUMNS = "u.user_name, p.project_name, b.dataset_name, b.id as dataset_id,"
            + " v.id, v.version_order, v.version_name, v.shared, v.created_time, v.modified_time";

    @SelectProvider(value = DatasetVersionProvider.class, method = "listSql")
    List<DatasetVersionEntity> list(@Param("datasetId") Long datasetId,
            @Param("namePrefix") String namePrefix,
            @Param("tag") String tag);

    @Select("select " + COLUMNS + " from dataset_version where id = #{id}")
    DatasetVersionEntity find(@Param("id") Long id);

    @Select("select " + COLUMNS + " from dataset_version where id in (${ids})")
    List<DatasetVersionEntity> findByIds(@Param("ids") String ids);

    @Select("select " + COLUMNS + " from dataset_version where status = #{status}")
    List<DatasetVersionEntity> findByStatus(@Param("status") Integer status);

    @Select("select " + COLUMNS + " from dataset_version"
            + " where dataset_id = #{datasetId}"
            + " order by version_order desc"
            + " limit 1")
    DatasetVersionEntity findByLatest(@Param("datasetId") Long datasetId);

    @SelectProvider(value = DatasetVersionProvider.class, method = "findByNameAndDatasetIdSql")
    DatasetVersionEntity findByNameAndDatasetId(@Param("versionName") String versionName,
            @Param("datasetId") Long datasetId,
            @Param("forUpdate") Boolean forUpdate);

    @Select("select " + COLUMNS + " from dataset_version"
            + " where version_order = #{versionOrder}"
            + " and dataset_id = #{datasetId}")
    DatasetVersionEntity findByVersionOrder(@Param("versionOrder") Long versionOrder,
            @Param("datasetId") Long datasetId);

    @Select("select version_order from dataset_version where id = #{id} for update")
    Long selectVersionOrderForUpdate(@Param("id") Long id);

    @Select("select max(version_order) as max from dataset_version where dataset_id = #{datasetId} for update")
    Long selectMaxVersionOrderOfDatasetForUpdate(@Param("datasetId") Long datasetId);

    @Update("update dataset_version set version_order = #{versionOrder} where id = #{id}")
    int updateVersionOrder(@Param("id") Long id, @Param("versionOrder") Long versionOrder);

    @Insert("insert into dataset_version(dataset_id, owner_id, version_name, size,"
            + " index_table, version_tag, version_meta, storage_path, files_uploaded)"
            + " values (#{datasetId}, #{ownerId}, #{versionName}, #{size},"
            + " #{indexTable}, #{versionTag}, #{versionMeta}, #{storagePath}, #{filesUploaded})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(DatasetVersionEntity version);

    @UpdateProvider(value = DatasetVersionProvider.class, method = "updateSql")
    int update(DatasetVersionEntity version);

    @Update("update dataset_version set version_tag = #{tag} where id = #{id}")
    int updateTag(@Param("id") Long id, @Param("tag") String tag);

    @Update("update dataset_version set shared = #{shared} where id = #{id}")
    int updateShared(@Param("id") Long id, @Param("shared") Boolean shared);

    //int updateFilesUploaded(@Param("version") DatasetVersionEntity version);
    @Update("update dataset_version set files_uploaded = #{filesUploaded} where id = #{id}")
    int updateFilesUploaded(@Param("id") Long id, @Param("filesUploaded") String filesUploaded);

    @Update("update dataset_version set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("delete from dataset_version where id = #{id}")
    int delete(@Param("id") Long id);

    @Select("select " + VERSION_VIEW_COLUMNS
            + " from dataset_version as v, dataset_info as b, project_info as p, user_info as u"
            + " where v.dataset_id = b.id"
            + " and b.project_id = p.id"
            + " and p.owner_id = u.id"
            + " and p.is_deleted = 0"
            + " and p.id = #{projectId}"
            + " order by b.id desc, v.version_order desc")
    List<DatasetVersionViewEntity> listDatasetVersionViewByProject(@Param("projectId") Long projectId);

    @Select("select " + VERSION_VIEW_COLUMNS
            + " from dataset_version as v, dataset_info as b, project_info as p, user_info as u"
            + " where v.dataset_id = b.id"
            + " and b.project_id = p.id"
            + " and p.owner_id = u.id"
            + " and p.is_deleted = 0"
            + " and p.privacy = 1"
            + " and v.shared = 1"
            + " and p.id != #{excludeProjectId}"
            + " order by b.id desc, v.version_order desc")
    List<DatasetVersionViewEntity> listDatasetVersionViewByShared(@Param("excludeProjectId") Long excludeProjectId);

    class DatasetVersionProvider {

        public String listSql(@Param("datasetId") Long datasetId,
                @Param("namePrefix") String namePrefix,
                @Param("tag") String tag) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("dataset_version");
                    WHERE("dataset_id = #{datasetId}");
                    if (StrUtil.isNotEmpty(namePrefix)) {
                        WHERE("version_name like concat(#{namePrefix}, '%')");
                    }
                    if (StrUtil.isNotEmpty(tag)) {
                        WHERE("FIND_IN_SET(#{tag}, version_tag)");
                    }
                    ORDER_BY("version_order desc");
                }
            }.toString();
        }

        public String findByNameAndDatasetIdSql(@Param("datasetId") Long datasetId,
                @Param("versionName") String versionName,
                @Param("forUpdate") Boolean forUpdate) {
            String sql = new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("dataset_version");
                    WHERE("version_name = #{versionName}");
                    if (Objects.nonNull(datasetId)) {
                        WHERE("dataset_id = #{datasetId}");
                    }
                }
            }.toString();

            return Objects.equals(forUpdate, true) ? (sql + " for update") : sql;
        }

        public String updateSql(DatasetVersionEntity version) {
            return new SQL() {
                {
                    UPDATE("dataset_version");
                    if (StrUtil.isNotEmpty(version.getVersionTag())) {
                        SET("version_tag = #{versionTag}");
                    }
                    WHERE("where id = #{id}");
                }
            }.toString();
        }
    }
}
