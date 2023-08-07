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

package ai.starwhale.mlops.domain.report;

import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.bundle.remove.RemoveAccessor;
import org.springframework.stereotype.Service;

@Service
public class ReportDao implements BundleAccessor, RecoverAccessor, RemoveAccessor {
    private final ReportMapper reportMapper;

    public ReportDao(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    public BundleEntity findById(Long id) {
        return reportMapper.selectById(id);
    }

    @Override
    public BundleEntity findByNameForUpdate(String name, Long projectId) {
        return reportMapper.selectByName(name, projectId, true);
    }

    @Override
    public Type getType() {
        return Type.REPORT;
    }

    @Override
    public BundleEntity findDeletedBundleById(Long id) {
        return reportMapper.findDeleted(id);
    }

    @Override
    public Boolean recover(Long id) {
        return reportMapper.recover(id) > 0;
    }

    @Override
    public Boolean remove(Long id) {
        return reportMapper.recover(id) > 0;
    }
}
