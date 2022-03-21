/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index.mnist;

import ai.starwhale.mlops.common.util.ArrayHelper;
import ai.starwhale.mlops.domain.storage.StorageAccessService;
import ai.starwhale.mlops.domain.swds.SWDSIndex.SWDSBlock;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.SWDSIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * refer to http://yann.lecun.com/exdb/mnist/
 * MINST_PATH/image_file
 * MINST_PATH/label_file
 * MNIST_PATH_INDEX
 */
public class MNISTSpliter {

    private static final int LABEL_FILE_MAGIC_NUMBER = 2049;
    private static final int IMAGE_FILE_MAGIC_NUMBER = 2051;

    StorageAccessService storageAccessService;

    ObjectMapper objectMapper;

    /**
     * split the images file and labels file to smaller size and store them into the storage
     * @param swds the MNIST DS
     * @param blockSize bolck size to be split
     * @return the index directory path of the DS
     */
    public SWDSIndex splitMNIST(SWDataSet swds,int blockSize) throws IOException {
        final Integer swdsSize = swds.getSize();

        final String swdsPath = swds.getPath();
        //label file & image file
        Stream<String> files = storageAccessService.list(swdsPath);
        //assert two files got
        final Map<Boolean, MNISTFile> mnistFiles = files.map(path -> new MNISTFile(path))
            .collect(Collectors.toMap(MNISTFile::getLabel, Function.identity()));

        final String indexPath = allocateIndexPath(swdsPath);
        if(swdsSize <= blockSize){
            final SWDSIndex swdsIndex = SWDSIndex.builder()
                .storagePath(indexPath)
                .build().add(SWDSBlock.builder()
                    .offset(0)
                    .pathImage(mnistFiles.get(Boolean.FALSE).getPath())
                    .pathLabel(mnistFiles.get(Boolean.TRUE).getPath())
                    .size(swdsSize)
                    .build());
            wiriteIndexItemsToStorage(swdsIndex);
            return swdsIndex;
        }

        final List<Integer> slicePoints = ArrayHelper.INSTANCE.slicePoints(swdsSize, blockSize);

        //TODO

        SWDSIndex swdsIndex;
        return null;
    }

    private void wiriteIndexItemsToStorage(SWDSIndex swdsIndex) throws IOException {
        try(final OutputStream outputStream = storageAccessService.put(swdsIndex.getStoragePath());){
            objectMapper.writeValue(outputStream,swdsIndex.getSWDSBlockList());
        }

    }

    final static String SUFFIX_INDEX="_INDEX";
    private String allocateIndexPath(String swdsPath){
        return swdsPath + SUFFIX_INDEX;
    }

}
