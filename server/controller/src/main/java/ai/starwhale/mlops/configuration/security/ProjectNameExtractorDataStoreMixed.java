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

package ai.starwhale.mlops.configuration.security;

import ai.starwhale.mlops.api.protocol.datastore.ListTablesRequest;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.util.HttpUtil;
import ai.starwhale.mlops.common.util.HttpUtil.Resources;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.exception.SwNotFoundException;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


/**
 * this implementation relays on ContentCachingFilter
 */
@Slf4j
@Component
public class ProjectNameExtractorDataStoreMixed implements ProjectNameExtractor {

    final String apiPrefix;

    final ObjectMapper objectMapper;

    private final ProjectService projectService;

    private final IdConverter idConverter;
    private final JobDao jobDao;
    private final ModelDao modelDao;
    private final DatasetDao datasetDao;
    private final RuntimeDao runtimeDao;

    static final String PATH_LIST_TABLES = "/datastore/listTables";
    static final String PATH_UPDATE_TABLE = "/datastore/updateTable";
    static final String PATH_QUERY_TABLE = "/datastore/queryTable";
    static final String PATH_SCAN_TABLE = "/datastore/scanTable";

    private static final Pattern RESOURCE_PATTERN =
            Pattern.compile("^/project/([^/]+)/(runtime|job|dataset|model)/([^/]+).*$");

    public ProjectNameExtractorDataStoreMixed(
            @Value("${sw.controller.api-prefix}") String apiPrefix,
            ObjectMapper objectMapper,
            ProjectService projectService,
            IdConverter idConverter,
            JobDao jobDao,
            ModelDao modelDao,
            DatasetDao datasetDao,
            RuntimeDao runtimeDao
    ) {
        this.apiPrefix = StringUtils.trimTrailingCharacter(apiPrefix, '/');
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.idConverter = idConverter;
        this.jobDao = jobDao;
        this.modelDao = modelDao;
        this.datasetDao = datasetDao;
        this.runtimeDao = runtimeDao;
    }

    @Override
    public Set<String> extractProjectName(HttpServletRequest request) {
        return isDataStore(request) ? projectsOfDatastore(request) : projectsOfNoneDataStore(request);
    }

    private Set<String> projectsOfDatastore(HttpServletRequest request) {
        String path = request.getRequestURI().replace(apiPrefix, "");
        try {
            ServletInputStream wrappedInputStream = request.getInputStream();
            byte[] bytes = inputStreamToBytes(wrappedInputStream);
            if (PATH_LIST_TABLES.equals(path)) {
                ListTablesRequest req = objectMapper.readValue(bytes, ListTablesRequest.class);
                return singleToSet(tableName2ProjectName(req.getPrefix()));
            } else if (PATH_UPDATE_TABLE.equals(path)) {
                UpdateTableRequest req = objectMapper.readValue(bytes, UpdateTableRequest.class);
                return singleToSet(tableName2ProjectName(req.getTableName()));
            } else if (path.startsWith(PATH_QUERY_TABLE)) {
                QueryTableRequest req = objectMapper.readValue(bytes, QueryTableRequest.class);
                return singleToSet(tableName2ProjectName(req.getTableName()));
            } else if (PATH_SCAN_TABLE.equals(path)) {
                ScanTableRequest req = objectMapper.readValue(bytes, ScanTableRequest.class);
                return req.getTables().stream()
                        .map(tableDesc -> tableName2ProjectName(tableDesc.getTableName()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        } catch (IOException e) {
            log.error("", e);

        }
        return Set.of();
    }

    byte[] inputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int read;
        byte[] data = new byte[16384];

        while ((read = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, read);
        }

        return buffer.toByteArray();
    }

    static final Pattern PATTERN_TABLE_NAME = Pattern.compile("^project\\/([^\\/]*)\\/?.*$");

    String tableName2ProjectName(String tableName) {
        Matcher matcher = PATTERN_TABLE_NAME.matcher(tableName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            log.warn("table not match pattern ^project\\/([^\\/]*)\\/.*$  {}", tableName);
            return null;
        }
    }

    Set<String> singleToSet(String project) {
        if (!StringUtils.hasText(project)) {
            return Set.of();
        }
        return Set.of(project);
    }

    boolean isDataStore(HttpServletRequest request) {
        return request.getRequestURI().startsWith(this.apiPrefix + "/datastore");
    }

    public static String SYSTEM_PROJECT = "0";

    private Set<String> projectsOfNoneDataStore(HttpServletRequest request) {
        var projectUrl = HttpUtil.getResourceUrlFromPath(request.getRequestURI(), Resources.PROJECT);
        if (StrUtil.isEmpty(projectUrl)) {
            return Set.of(SYSTEM_PROJECT);
        }

        checkResourceOwnerShip(request);
        return Set.of(URLDecoder.decode(projectUrl, Charset.defaultCharset()));
    }

    public void checkResourceOwnerShip(HttpServletRequest request) {
        if (isDataStore(request)) {
            return;
        }
        String path = request.getRequestURI().replace(apiPrefix, "");
        Matcher matcher = RESOURCE_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return;
        }
        String project = matcher.group(1);
        var projectEntity = projectService.findProject(project);
        if (projectEntity == null) {
            throw new SwNotFoundException(SwNotFoundException.ResourceType.PROJECT, project);
        }

        String resourceType = matcher.group(2);
        String resourceUrl = matcher.group(3);
        var accessor = Map.of(
                "runtime", runtimeDao,
                "job", jobDao,
                "dataset", datasetDao,
                "model", modelDao
        );
        if (!accessor.containsKey(resourceType)) {
            return;
        }

        var dao = accessor.get(resourceType);

        BundleEntity resource;
        if (!idConverter.isId(resourceUrl)) {
            // no need to validate resource with name, the biz will always touch the resource with project id
            // BUT job has uuid, it is a special case
            if (!resourceType.equals("job")) {
                return;
            }
            resource = dao.findByNameForUpdate(resourceUrl, projectEntity.getId());
        } else {
            resource = dao.findById(idConverter.revert(resourceUrl));
        }
        if (resource == null || !resource.getProjectId().equals(projectEntity.getId())) {
            throw new SwNotFoundException(SwNotFoundException.ResourceType.BUNDLE, resourceUrl);
        }
    }
}
