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

package ai.starwhale.mlops.datastore;

import java.text.SimpleDateFormat;
import java.util.Random;

public abstract class TestThread extends Thread {

    protected final Random random = new Random();
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
    private Throwable throwable;

    public void run() {
        try {
            this.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            this.throwable = t;
        }
    }

    public abstract void execute() throws Exception;

    public void checkException() throws Throwable {
        if (this.throwable != null) {
            throw this.throwable;
        }
    }
}
