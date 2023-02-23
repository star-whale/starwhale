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

import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import io.jsonwebtoken.Claims;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RuntimeTokenValidator implements JwtClaimValidator {
    private static final String CLAIM_KEY = "runtime-id";
    private final JwtTokenUtil jwtTokenUtil;

    private final RuntimeDao runtimeDao;

    public RuntimeTokenValidator(JwtTokenUtil jwtTokenUtil, RuntimeDao runtimeDao) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.runtimeDao = runtimeDao;
    }

    public String getToken(User owner, Long versionId) {
        String jwtToken = jwtTokenUtil.generateAccessToken(owner, Map.of(CLAIM_KEY, versionId));
        return String.format("Bearer %s", jwtToken);
    }

    @Override
    public void validClaims(Claims claims) throws SwValidationException {
        var val = claims.get(CLAIM_KEY);
        if (null == val) {
            return;
        }
        long id;
        try {
            id = ((Number) val).longValue();
        } catch (ClassCastException e) {
            throw new SwValidationException(ValidSubject.USER, "invalid id for model serving");
        }
        var rt = runtimeDao.getRuntimeVersion(String.valueOf(id));
        if (rt == null) {
            throw new SwValidationException(ValidSubject.USER, "can not find runtime by version");
        }
    }
}
