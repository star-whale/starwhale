/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import com.github.pagehelper.PageInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SWModelPackageController implements SWModelPackageApi{

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(String modelName,
        Integer pageNum, Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> revertModelVersion(String modelId,
        String versionId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteModelById(Integer modelId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<SWModelPackageInfoVO>> getModelInfo(Integer modelId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVersionVO>>> listModelVersion(
        Integer modelId, String modelVersionName, Integer pageNum, Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createModelVersion(String modelId,
        MultipartFile zipFile, String importPath) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyModel(String modelId, String versionId,
        String tag) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createModel(String modelName,
        MultipartFile zipFile, @RequestParam(value = "importPath") String importPath) {
        return null;
    }
}
