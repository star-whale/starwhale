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

package ai.starwhale.mlops.schedule.k8s;

import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class K8SJobTemplate {

    public static final Map<String, String> starwhaleJobLabel = Map.of("owner", "starwhale");

    public static final String jobIdentityLabel = "job-name";

    public static final String pipCacheVolumeName = "pip-cache";

    @Value("${sw.infra.k8s.host-path-for-cache}")
    private String pipCacheHostPath;

    final String template;
    final V1Job v1Job;

    public K8SJobTemplate(@Value("${sw.infra.k8s.job-template-path}") String templatePath)
        throws IOException {
        if(!StringUtils.hasText(templatePath)){
            this.template = getJobDefaultTemplate();
        }else {
            this.template = Files.readString(Paths.get(templatePath));
        }
        v1Job = Yaml.loadAs(template, V1Job.class);
    }

    public List<V1Container> getInitContainerTemplates(){
        return v1Job.getSpec().getTemplate().getSpec().getInitContainers();
    }

    public List<V1Container> getContainersTemplates(){
        return v1Job.getSpec().getTemplate().getSpec().getContainers();
    }

    public V1Job renderJob(String jobName,
        Map<String, ContainerOverwriteSpec> containerSpecMap,Map<String,String> nodeSelectors) {
        V1Job job = Yaml.loadAs(template, V1Job.class);
        job.getMetadata().name(jobName);
        HashMap<String, String> labels = new HashMap<>();
        labels.putAll(starwhaleJobLabel);
        labels.put(jobIdentityLabel,jobName);
        job.getMetadata().labels(labels);
        V1JobSpec jobSpec = job.getSpec();
        Objects.requireNonNull(jobSpec, "can not get job spec");
        V1PodSpec podSpec = jobSpec.getTemplate().getSpec();
        Objects.requireNonNull(podSpec, "can not get pod spec");
        if(null != nodeSelectors){
            Map<String, String> templateSelector = podSpec.getNodeSelector();
            if(null != templateSelector){
                nodeSelectors.putAll(templateSelector);
            }
            podSpec.nodeSelector(nodeSelectors);
        }
        Stream.concat(podSpec.getContainers().stream(),podSpec.getInitContainers().stream()).forEach(c->{
            ContainerOverwriteSpec containerOverwriteSpec = containerSpecMap.get(c.getName());
            if(null == containerOverwriteSpec){
                return;
            }
            if(null != containerOverwriteSpec.resourceOverwriteSpec
                && null != containerOverwriteSpec.resourceOverwriteSpec.getResourceSelector()){
                c.resources(containerOverwriteSpec.resourceOverwriteSpec.getResourceSelector());
            }
            if(!CollectionUtils.isEmpty(containerOverwriteSpec.cmds)){
                c.args(containerOverwriteSpec.cmds);
            }
            if(StringUtils.hasText(containerOverwriteSpec.image)){
                c.image(containerOverwriteSpec.image);
            }
            if(!CollectionUtils.isEmpty(containerOverwriteSpec.envs)){
                c.env(containerOverwriteSpec.envs);
            }

        });

        // replace host path
        List<V1Volume> volumes = job.getSpec().getTemplate().getSpec().getVolumes();
        volumes.stream().filter(v -> v.getName().equals(pipCacheVolumeName))
            .findFirst().ifPresent(volume -> volume.getHostPath().path(pipCacheHostPath));

        return job;
    }

    private String getJobDefaultTemplate() throws IOException {
        String file = "template/job.yaml";
        InputStream is = this.getClass().getClassLoader()
            .getResourceAsStream(file);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
