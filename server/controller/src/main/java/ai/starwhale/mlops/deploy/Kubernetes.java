package ai.starwhale.mlops.deploy;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Kubernetes {
    private ApiClient client;
    private CoreV1Api coreV1Api;
    private final BatchV1Api batchV1Api;
    private final String ns;

    /**
     * Basic constructor for Kubernetes
     */
    public Kubernetes(String ns) throws IOException {
        client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        coreV1Api = new CoreV1Api();
        batchV1Api = new BatchV1Api();
        this.ns = ns;
    }

    /**
     * deploy apply job to k8s with in this.ns
     *
     * @param job to apply
     * @return submitted job
     */
    public V1Job deploy(V1Job job) throws ApiException {
        return batchV1Api.createNamespacedJob(ns, job, null, null, null, null);
    }

    /**
     * renderJob parses from job yaml template
     *
     * @param template
     * @param containerName
     * @param cmd
     * @return
     */
    public V1Job renderJob(String template, String name, String containerName, String image, List<String> cmd, Map<String, String> env) {
        V1Job job = Yaml.loadAs(template, V1Job.class);
        job.getMetadata().name(name);
        V1JobSpec jobSpec = job.getSpec();
        Objects.requireNonNull(jobSpec, "can not get job spec");
        V1PodSpec podSpec = jobSpec.getTemplate().getSpec();
        Objects.requireNonNull(podSpec, "can not get pod spec");

        V1Container container = podSpec.getContainers().stream().filter(c -> c.getName().equals(containerName)).findFirst().orElse(null);
        Objects.requireNonNull(container, "can not get container by name " + containerName);

        if (!image.isEmpty()) {
            container.image(image);
        }
        if (!cmd.isEmpty()) {
            container.args(cmd);
        }
        if (!env.isEmpty()) {
            List<V1EnvVar> ee = new ArrayList<>();
            env.forEach((k, v) -> ee.add(new V1EnvVar().name(k).value(v)));
            podSpec.getInitContainers().forEach(c -> c.env(ee));
        }

        return job;
    }

    /**
     * get all jobs with in this.ns
     *
     * @return job list
     */
    public V1JobList get() throws ApiException {
        return batchV1Api.listNamespacedJob(ns, null, null, null, null, null, null, null, null, null, null);
    }
}
