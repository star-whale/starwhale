---
title: Evaluation
---
## Do evaluation on the server

### Overwrite the step specification

You could submit a step specification yaml to server when you create a job to overwrite the hard coded step in `swmp` . The `resources` field behaves exactly the same as [K8S Resource Management for Pods and Containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)

```yaml
  - job_name: default
    needs: [ ]
    resources:
      - type: cpu # nvidia.com/gpu, memory
        request: 1 # float
        limit: 1 # float
    step_name: ppl
    task_num: 2
  - job_name: default
    needs:
      - ppl
    resources:
      - type: cpu
        request: 1
        limit: 1
    step_name: cmp
    task_num: 1
```
