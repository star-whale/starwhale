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

package ai.starwhale.mlops.api.protocol.job;


import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.common.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

class JobRequestTest {

    @Test
    public void testDeserialize() throws JsonProcessingException {
        try (var f = buildDefaultValidatorFactory()) {
            // fill required fields only
            JobRequest jobRequest = new JobRequest();
            jobRequest.setModelVersionUrl("model version");
            jobRequest.setResourcePool("default");

            var json = "{\"modelVersionUrl\":\"model version\",\"resourcePool\":\"default\"}";
            var deserialized = Constants.objectMapper.readValue(json, JobRequest.class);
            assertEquals(jobRequest, deserialized);
            Validator validator = f.getValidator();
            var violations = validator.validate(deserialized);
            assertEquals(0, violations.size());

            // missing required fields
            json = "{\"resourcePool\":\"default\"}";
            deserialized = Constants.objectMapper.readValue(json, JobRequest.class);
            violations = validator.validate(deserialized);
            assertEquals(1, violations.size());
        }
    }
}
