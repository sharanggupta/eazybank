{{/*
Expand the name of the chart.
*/}}
{{- define "eazybank-service.name" -}}
{{- default .Chart.Name .Values.service.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "eazybank-service.fullname" -}}
{{- if .Values.service.name }}
{{- .Values.service.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eazybank-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "eazybank-service.labels" -}}
helm.sh/chart: {{ include "eazybank-service.chart" . }}
{{ include "eazybank-service.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.image.tag | default .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "eazybank-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eazybank-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eazybank-service.serviceAccountName" -}}
{{- if .Values.k8s.serviceAccount.create }}
{{- default (include "eazybank-service.fullname" .) .Values.k8s.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.k8s.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL fullname
*/}}
{{- define "eazybank-service.postgresql.fullname" -}}
{{- printf "%s-postgresql" (include "eazybank-service.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "eazybank-service.postgresql.labels" -}}
helm.sh/chart: {{ include "eazybank-service.chart" . }}
{{ include "eazybank-service.postgresql.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
PostgreSQL selector labels
*/}}
{{- define "eazybank-service.postgresql.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eazybank-service.postgresql.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
{{- end }}

{{/*
Generate datasource URL
*/}}
{{- define "eazybank-service.datasourceUrl" -}}
{{- if .Values.app.datasource.url }}
{{- .Values.app.datasource.url }}
{{- else if .Values.postgresql.enabled }}
{{- printf "jdbc:postgresql://%s:5432/%s" (include "eazybank-service.postgresql.fullname" .) .Values.postgresql.database }}
{{- else }}
{{- fail "Either app.datasource.url or postgresql.enabled must be set" }}
{{- end }}
{{- end }}
