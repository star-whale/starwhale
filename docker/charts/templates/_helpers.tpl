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
