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
      {{- with .Values.image.pullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8}}
      {{- end }}
      serviceAccountName: {{ include "chart.serviceAccountName" . }}
      volumes:
        - name: agent-storage
          hostPath:
          {{if eq .role "gpu"}}
            path: {{ .Values.storage.agentGPUHostPath }}
          {{else}}
            path: {{ .Values.storage.agentCPUHostPath }}
          {{end}}
            type: DirectoryOrCreate
      nodeSelector:
      {{if eq .role "gpu"}}
        {{- toYaml .Values.nodeSelector.agentGPU | nindent 8}}
      {{else}}
        {{- toYaml .Values.nodeSelector.agentCPU | nindent 8}}
      {{end}}
      containers:
        - name: agent
          image: "{{ .Values.image.registry}}/starwhaleai/taskset:{{ .Values.image.tag | default .Chart.AppVersion }}"
          command: ["sleep"]
          args: ["3600000"]
          env:
            - name: DOCKER_HOST
              value: "tcp://127.0.0.1:2376"
          volumeMounts:
            - name: agent-storage
              mountPath: "/opt/starwhale"
              subPath: run
        - name: taskset
          image: "{{ .Values.image.registry}}/starwhaleai/taskset:{{ .Values.image.tasksetTag | default .Chart.AppVersion }}"
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
          resources:
          {{if eq .role "gpu"}}
            {{- toYaml .Values.resources.agentGPU | nindent 12 }}
          {{else}}
            {{- toYaml .Values.resources.agentCPU | nindent 12 }}
          {{end}}
{{- end}}