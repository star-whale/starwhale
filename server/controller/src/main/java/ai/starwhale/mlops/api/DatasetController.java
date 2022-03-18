/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import com.github.pagehelper.PageInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DatasetController implements DatasetApi{

    @Override
    public ResponseEntity<ResponseMessage<String>> revertDatasetVersion(String datasetId,
        String versionId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteDatasetById(Integer datasetId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<DatasetVersionVO>> getDatasetInfo(Integer datasetId) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVersionVO>>> listDatasetVersion(
        Integer datasetId, Integer pageNum, Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<Void> createDatasetVersion(String datasetId, MultipartFile zipFile,
        String importPath) {
        return null;
    }

    @Override
    public ResponseEntity<Void> modifyDatasetVersionInfo(String datasetId, String versionId,
        String tag) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<DatasetVO>>> listDataset(Integer pageNum,
        Integer pageSize) {
        return null;
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createDataset(String datasetName,
        MultipartFile zipFile, String importPath) {
        return null;
    }
}
