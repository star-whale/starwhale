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

import ai.starwhale.mlops.api.protobuf.Dataset.BuildStatus;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.dataset.build.mapper.BuildRecordMapper;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import io.jsonwebtoken.Claims;
import java.util.Map;
import org.springframework.stereotype.Component;


@Component
public class DatasetBuildTokenValidator implements JwtClaimValidator {

    private static final String CLAIM_ID = "buildRecordId";
    final JwtTokenUtil jwtTokenUtil;
    final BuildRecordMapper mapper;

    public DatasetBuildTokenValidator(JwtTokenUtil jwtTokenUtil, BuildRecordMapper mapper) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.mapper = mapper;
    }

    public String getToken(User owner, Long taskId) {
        String jwtToken = jwtTokenUtil.generateAccessToken(owner, Map.of(CLAIM_ID, taskId));
        return String.format("Bearer %s", jwtToken);
    }

    @Override
    public void validClaims(Claims claims) throws SwValidationException {
        Object claimId = claims.get(CLAIM_ID);
        if (null == claimId) {
            return;
        }
        long id;
        try {
            id = ((Number) claimId).longValue();
        } catch (ClassCastException e) {
            throw new SwValidationException(ValidSubject.USER, "dataset build record claim invalid");
        }
        var record = mapper.selectById(id);
        if (null == record || BuildStatus.BUILDING != record.getStatus()) {
            throw new SwValidationException(ValidSubject.USER, "dataset build record claim invalid");
        }
    }
}
