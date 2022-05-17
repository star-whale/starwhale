{{/*
Expand the name of the chart.
*/}}
{{- define "chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "chart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "chart.labels" -}}
helm.sh/chart: {{ include "chart.chart" . }}
{{ include "chart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
controller label
*/}}
{{- define "chart.controller.labels" -}}
starwhale.ai/role: controller
app.kubernetes.io/name: {{ include "common.names.fullname" . }}
app.kubernetes.io/instance: {{ include "common.names.fullname" . }}-controller
{{- end}}

{{/*
Selector labels
*/}}
{{- define "chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "chart.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "chart.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Config mirror environment
*/}}
{{- define "chart.mirror.env" -}}
- name: SW_RESET_CONDA_CONFIG
{{- if .Values.mirror.conda.enabled }}
  value: "0"
{{- else }}
  value: "1"
{{- end }}

{{- if .Values.mirror.pypi.enabled }}
- name: SW_PYPI_INDEX_URL
  value: "{{ .Values.mirror.pypi.indexUrl }}"
- name: SW_PYPI_EXTRA_INDEX_URL
  value: "{{ .Values.mirror.pypi.extraIndexUrl }}"
- name: SW_PYPI_TRUSTED_HOST
  value: "{{ .Values.mirror.pypi.trustedHost }}"
{{- end}}
{{- end}}

{{/*
Create Agent Daemonset
*/}}
{{- define "chart.agent" -}}
metadata:
  name: {{ include "common.names.fullname" . }}-agent-{{ .role }}
  namespace: {{ .Release.Namespace }}
  labels: {{ include "common.labels.standard" . | nindent 4 }}
  {{- if .Values.commonLabels }}
  {{- include "common.tplvalues.render" ( dict "value" .Values.commonLabels "context" $ ) | nindent 4 }}
  {{- end }}
    starwhale.ai/role: {{ .role }}

spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: {{ include "common.names.fullname" . }}
      app.kubernetes.io/instance: {{ include "common.names.fullname" . }}-agent
      starwhale.ai/role: {{ .role }}
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ include "common.names.fullname" . }}
        app.kubernetes.io/instance: {{ include "common.names.fullname" . }}-agent
        starwhale.ai/role: {{ .role }}
    spec:
      serviceAccountName: {{ include "chart.serviceAccountName" . }}
      volumes:
        - name: agent-storage
          hostPath:
        {{- if .Values.minikube.enabled }}
          {{- if eq .role "gpu"}}
            path: {{ .Values.minikube.agentHostPath}}/agent-gpu
          {{- else}}
            path: {{ .Values.minikube.agentHostPath}}/agent-cpu
          {{- end}}
        {{- else }}
          {{- if eq .role "gpu"}}
            path: {{ .Values.storage.agentHostPathRoot }}/agent-gpu
          {{- else}}
            path: {{ .Values.storage.agentHostPathRoot }}/agent-cpu
          {{- end}}
        {{- end}}
            type: DirectoryOrCreate
    {{- if not .Values.minikube.enabled }}
      nodeSelector:
      {{- if eq .role "gpu"}}
        {{- toYaml .Values.nodeSelector.agentGPU | nindent 8}}
      {{else}}
        {{- toYaml .Values.nodeSelector.agentCPU | nindent 8}}
      {{- end}}
    {{- end}}
      containers:
        - name: agent
          image: "{{ .Values.image.registry}}/{{ .Values.image.agent.repo }}:{{ .Values.image.agent.tag | default .Chart.AppVersion }}"
          env:
            {{ include "chart.mirror.env" . | nindent 12 }}
            - name: SW_HOST_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
            - name: SW_CONTROLLER_URL
              value: "http://{{ include "common.names.fullname" . }}-controller:{{ .Values.controller.containerPort }}/"
            - name: SW_BASE_PATH
              value: "/opt/starwhale"
            - name: DOCKER_HOST
              value: "tcp://127.0.0.1:2376"
            - name: SW_STORAGE_PREFIX
              value: "{{ include "common.names.fullname" . }}"
            {{- if .Values.minio.enabled}}
            - name: SW_STORAGE_ENDPOINT
              value: "http://{{ include "common.names.fullname" . }}-minio:{{ .Values.minio.containerPorts.api }}"
            - name: SW_STORAGE_BUCKET
              value: "{{ .Values.minio.defaultBuckets }}"
            - name: SW_STORAGE_ACCESSKEY
              value: "{{ .Values.minio.auth.rootUser }}"
            - name: SW_STORAGE_SECRETKEY
              value: "{{ .Values.minio.auth.rootPassword }}"
            - name: SW_STORAGE_REGION
              value: "local"
            {{- else }}
            - name: SW_STORAGE_ENDPOINT
              value: "http://{{ .Values.externalS3OSS.host }}:{{ .Values.externalS3OSS.port }}"
            - name: SW_STORAGE_BUCKET
              value: "{{ .Values.externalS3OSS.defaultBuckets }}"
            - name: SW_STORAGE_ACCESSKEY
              value: "{{ .Values.externalS3OSS.accessKey }}"
            - name: SW_STORAGE_SECRETKEY
              value: "{{ .Values.externalS3OSS.secretKey }}"
            - name: SW_STORAGE_REGION
              value: "{{ .Values.externalS3OSS.region }}"
            {{- end}}
          volumeMounts:
            - name: agent-storage
              mountPath: "/opt/starwhale"
              subPath: run
            - name: agent-storage
              mountPath: "/var/lib/docker"
              subPath: dind
        - name: taskset
          image: "{{ .Values.image.registry}}/{{ .Values.image.taskset.repo }}:{{ .Values.image.taskset.tag | default .Chart.AppVersion }}"
          env:
            {{ include "chart.mirror.env" . | nindent 12 }}
            - name: SW_HOST_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
          volumeMounts:
            - name: agent-storage
              mountPath: "/opt/starwhale"
              subPath: run
            - name: agent-storage
              mountPath: "/var/lib/docker"
              subPath: dind
          securityContext:
            privileged: true
          stdin: true
          tty: true
        {{- if not .Values.minikube.enabled }}
          resources:
          {{- if eq .role "gpu"}}
            {{- toYaml .Values.resources.agentGPU | nindent 12 }}
          {{- else}}
            {{- toYaml .Values.resources.agentCPU | nindent 12 }}
          {{- end}}
        {{- end }}
{{- end}}

{{/*
Create PV for minikube local environment
*/}}
{{- define "chart.minikubePV" -}}
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: {{ include "common.names.fullname" . }}-pv-{{ .backend }}
  namespace: {{ .Release.Namespace }}
spec:
  capacity:
    storage: {{ .Values.minikube.pv.storage }}
  volumeMode: Filesystem
  accessModes:
  - ReadWriteOnce
  hostPath:
    path: {{ .Values.minikube.pv.rootPath }}/{{ .backend }}
    type: DirectoryOrCreate
  storageClassName: local-storage-{{ .backend }}
---
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: local-storage-{{ .backend }}
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
{{- end}}