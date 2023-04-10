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

import ai.starwhale.mlops.domain.lock.ControllerLock;
import ai.starwhale.mlops.domain.lock.RequestLockFilter;
import javax.annotation.Resource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class FilterConfiguration {

    @Resource
    private ControllerLock lock;

    @Bean
    @Order(-1)
    public FilterRegistrationBean<RequestLockFilter> filterRegister() {
        FilterRegistrationBean<RequestLockFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestLockFilter(lock));
        bean.addUrlPatterns("/*");
        return bean;
    }

}
