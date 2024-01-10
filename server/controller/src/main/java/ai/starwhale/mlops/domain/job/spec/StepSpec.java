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

import ai.starwhale.mlops.domain.job.spec.svc.ServiceSpec;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepSpec {

    /**
     * the name of job which would be executed at job yaml during running time.
     * server side only.
     */
    @JsonProperty("job_name")
    private String jobName;

    @NotNull
    private String name;

    @NotNull
    @JsonProperty("show_name")
    private String showName;

    private Integer concurrency = 1;

    @NotNull
    private Integer replicas = 1;

    private Integer backoffLimit;

    private List<String> needs;

    private List<RuntimeResource> resources;

    private List<Env> env;

    // https://github.com/star-whale/starwhale/pull/2350
    private Integer expose;
    private Boolean virtual;

    @JsonProperty("require_dataset")
    private Boolean requireDataset;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FineTune {
        @JsonProperty("require_train_datasets")
        private Boolean requireTrainDatasets;
        @JsonProperty("require_validation_datasets")
        private Boolean requireValidationDatasets;
    }

    @JsonProperty("fine_tune")
    private FineTune finetune;

    @JsonProperty("container_spec")
    ContainerSpec containerSpec;

    @JsonProperty("ext_cmd_args")
    private String extCmdArgs;

    @JsonProperty("parameters_sig")
    private List<ParameterSignature> parametersSig;

    @JsonProperty("service_spec")
    private ServiceSpec serviceSpec;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptionType {
        private String name;

        @JsonProperty("param_type")
        private String paramType;

        private List<String> choices;

        @JsonProperty("case_sensitive")
        private boolean caseSensitive = false;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptionField {
        private String name;
        private OptionType type;
        private boolean required = false;
        private boolean multiple = false;

        @JsonProperty("default")
        private Object defaultValue;

        /**
         * used for server side only
         */
        private String value;

        private String help;

        @JsonProperty("is_flag")
        private boolean isFlag = false;

        private boolean hidden = false;
    }

    public Map<String, Map<String, OptionField>> arguments;

    public void verifyStepSpecArgs() {
        if (CollectionUtils.isEmpty(this.getParametersSig())) {
            return;
        }
        Options options = new Options();
        for (var sig : this.getParametersSig()) {
            Option option = Option.builder()
                    .option(sig.getName())
                    .longOpt(sig.getName())
                    .hasArg()
                    .required(sig.getRequired())
                    .build();
            options.addOption(option);
        }
        try {
            new DefaultParser().parse(options, this.getExtCmdArgs().split(" "));
        } catch (ParseException | IllegalArgumentException e) {
            throw new SwValidationException(ValidSubject.JOB, e.getMessage(), e);
        }
    }

    @JsonIgnore
    public String getFriendlyName() {
        return StringUtils.hasText(showName) ? showName : name;
    }
}
