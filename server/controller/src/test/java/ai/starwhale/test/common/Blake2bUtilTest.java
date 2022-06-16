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

package ai.starwhale.test.common;

import ai.starwhale.mlops.common.util.Blake2bUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Blake2bUtilTest {

    @Test
    public void testDigest(){
        Assertions.assertEquals("0b8ced80a1e4eeb92b343778504cb749d934a0dfe86f5cd53f7c456c6c9b9cab1b1e83f570556eb087da34f6d64d996a55ebf52e7707dc23d2832b9b8e7067b3",Blake2bUtil.digest(mockString().getBytes()));
    }

    private String mockString() {
        return "/*\n"
            + " * Copyright 2022 Starwhale, Inc. All Rights Reserved.\n"
            + " *\n"
            + " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + " * you may not use this file except in compliance with the License.\n"
            + " * You may obtain a copy of the License at\n"
            + " *\n"
            + " * http://www.apache.org/licenses/LICENSE-2.0\n"
            + " *\n"
            + " * Unless required by applicable law or agreed to in writing, software\n"
            + " * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + " * See the License for the specific language governing permissions and\n"
            + " * limitations under the License.\n"
            + " */";
    }

}
