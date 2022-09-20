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

package ai.starwhale.mlops.storage.s3;

/**
 * BotoS3Config is for boto3 configuration communicate with starwhale python sdk
 * see <a href="https://botocore.amazonaws.com/v1/documentation/api/latest/reference/config.html">doc of config</a>
 */
public class BotoS3Config {
    public final AddressingStyleType addressingStyle;

    public enum AddressingStyleType {
        VIRTUAL, AUTO, PATH
    }

    public BotoS3Config(AddressingStyleType addressingStyle) {
        this.addressingStyle = addressingStyle;
    }

    public String toEnvStr() {
        return "{\"addressing_style\": \"" + this.addressingStyle.name().toLowerCase() + "\"}";
    }
}
