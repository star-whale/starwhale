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

package ai.starwhale.mlops.domain.job.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModelServingSpecTest {
    private static final String ONE_RC_CONTENT = "---\n"
            + "resources:\n"
            + "- type: \"a\"\n"
            + "  request: 7.0\n"
            + "  limit: 8.0\n";

    private static final String MULTIPLE_RC_CONTENT = "---\n"
            + "resources:\n"
            + "- type: \"b\"\n"
            + "  request: 9.0\n"
            + "  limit: 10.0\n"
            + "- type: \"a\"\n"
            + "  request: 7.0\n"
            + "  limit: 8.0\n";

    @Test
    public void testParseAndDumps() throws JsonProcessingException {
        var spec = ModelServingSpec.fromYamlString(ONE_RC_CONTENT);
        var rc = ModelServingSpec.RuntimeResource.builder()
                .type("a")
                .request(7f)
                .limit(8f)
                .build();
        Assertions.assertEquals(ModelServingSpec.builder().resources(List.of(rc)).build(), spec);
        Assertions.assertEquals(spec.dumps(), ONE_RC_CONTENT);

        spec = ModelServingSpec.fromYamlString(MULTIPLE_RC_CONTENT);
        var rc2 = ModelServingSpec.RuntimeResource.builder()
                .type("b")
                .request(9f)
                .limit(10f)
                .build();
        Assertions.assertEquals(List.of(rc, rc2), spec.getResources());

        var rcWithNatureOrder = "---\n"
                + "resources:\n"
                + "- type: \"a\"\n"
                + "  request: 7.0\n"
                + "  limit: 8.0\n"
                + "- type: \"b\"\n"
                + "  request: 9.0\n"
                + "  limit: 10.0\n";
        Assertions.assertEquals(spec.dumps(), rcWithNatureOrder);
    }
}
