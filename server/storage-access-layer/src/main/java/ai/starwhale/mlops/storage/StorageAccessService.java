/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * provides file upload/ download /list services
 */
public interface StorageAccessService {

    StorageObjectInfo head(String path) throws IOException;
    void put(String path,InputStream inputStream) throws IOException;
    void put(String path,byte[] body) throws IOException;
    InputStream get(String path) throws IOException;
    Stream<String> list(String path) throws IOException;
    void delete(String path) throws IOException;
}
