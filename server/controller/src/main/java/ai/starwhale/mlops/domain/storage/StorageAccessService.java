/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * provides file upload/ download /list services
 */
public interface StorageAccessService {

    OutputStream put(String path);
    InputStream get(String path);
    Stream<String> list(String path);

}
