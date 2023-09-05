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

package ai.starwhale.mlops.datastore.exporter;

import ai.starwhale.mlops.datastore.RecordList;
import java.io.IOException;
import java.io.OutputStream;

public interface RecordsStreamingExporter {

    int exportTo(RecordList recordList, OutputStream outputStream)  throws IOException;

    String getFileSuffix();

    String getWebMediaType();

}
