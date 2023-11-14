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

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "sw.features")
public class FeaturesProperties {
    public static final String FEATURE_ONLINE_EVAL = "online-eval";
    public static final String FEATURE_JOB_PAUSE = "job-pause";
    public static final String FEATURE_JOB_RESUME = "job-resume";
    public static final String FEATURE_JOB_DEV = "job-dev";
    public static final String FEATURE_JOB_PROXY = "job-proxy";
    public static final String FEATURE_FINE_TUNE = "fine-tune";

    List<String> disabled;

    public boolean featureEnabled(String feat) {
        return disabled == null || !disabled.contains(feat);
    }

    public boolean isOnlineEvalEnabled() {
        return featureEnabled(FEATURE_ONLINE_EVAL);
    }

    public boolean isJobPauseEnabled() {
        return featureEnabled(FEATURE_JOB_PAUSE);
    }

    public boolean isJobResumeEnabled() {
        return featureEnabled(FEATURE_JOB_RESUME);
    }

    public boolean isJobDevEnabled() {
        return featureEnabled(FEATURE_JOB_DEV);
    }

    public boolean isJobProxyEnabled() {
        return featureEnabled(FEATURE_JOB_PROXY);
    }

    public boolean isFineTuneEnabled() {
        return featureEnabled(FEATURE_FINE_TUNE);
    }
}
