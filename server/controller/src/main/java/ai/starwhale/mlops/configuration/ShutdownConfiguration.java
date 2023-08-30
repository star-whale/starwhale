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

package ai.starwhale.mlops.configuration;

import ai.starwhale.mlops.datastore.DataStore;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShutdownConfiguration {

    @Slf4j
    static class TerminateBean {
        private final DataStore dataStore;

        TerminateBean(DataStore dataStore) {
            this.dataStore = dataStore;
        }

        @PreDestroy
        public void onDestroy() throws Exception {
            log.info("Start to execute preDestroy logic.");
            dataStore.terminate();
            log.info("Spring Container is destroyed!");
        }
    }

    @Bean
    public TerminateBean terminateBean(DataStore dataStore) {
        return new TerminateBean(dataStore);
    }
}
