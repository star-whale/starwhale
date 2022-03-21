/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import ai.starwhale.mlops.domain.swds.SWDSIndexBuilder;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import java.util.function.Consumer;

public class IndexBuilderMNIST implements SWDSIndexBuilder {

    @Override
    public void submitIndexBuildRequest(SWDataSet swds, Consumer<BuildResult> callback) {

    }
}
