import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'
import { TextLink } from '@/components/Link'
import { api } from '@/api'
import { useParams } from 'react-router-dom'

export default function FineTuneOverview() {
    const [t] = useTranslation()

    const { projectId, spaceId, fineTuneId } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)
    const { job } = info.data || {}

    const datasetUris = job?.datasetList?.map((v) => {
        const uri = `project/${1}/dataset/${v?.name}/version/${v?.version?.name}`
        const to = `/projects/${projectId}/datasets/${v?.id}/versions/${v?.version?.id}/overview`
        return (
            <TextLink key={uri} to={to} baseStyle={{ maxWidth: 'none' }}>
                {uri}
            </TextLink>
        )
    })

    const modelUri = `project/${1}/model/${job?.modelName}/version/${job?.modelVersion}`
    const modelTo = `/projects/${projectId}/models/${job?.model?.id}/versions/${job?.model?.version?.id}/overview`
    const modelLink = (
        <TextLink key={modelUri} to={modelTo} baseStyle={{ maxWidth: 'none' }}>
            {modelUri}
        </TextLink>
    )

    const runtimeUri = `project/${1}/runtime/${job?.runtime?.name}/version/${job?.runtime?.version?.name}`
    const runtimeTo = `/projects/${projectId}/runtimes/${job?.runtime?.id}/versions/${job?.runtime?.version?.id}/overview`
    const runtimeLink = (
        <TextLink key={runtimeUri} to={runtimeTo} baseStyle={{ maxWidth: 'none' }}>
            {runtimeUri}
        </TextLink>
    )

    const items = [
        {
            key: 'id',
            value: job?.uuid ?? '-',
        },
        {
            label: t('Job ID'),
            key: 'sys/id',
            value: job?.id ?? '-',
        },
        {
            label: t('Resource Pool'),
            key: 'sys/resource_pool',
            value: job?.resourcePool,
        },
        {
            label: t('sth name', [t('Model')]),
            key: 'sys/model_name',
            value: job?.modelName,
        },
        {
            key: 'sys/model_uri',
            value: modelLink,
        },
        {
            label: t('Model Version'),
            key: 'sys/model_version',
            value: job?.modelVersion,
        },
        {
            key: 'sys/model_version_id',
            value: job?.model?.version?.id,
        },
        {
            label: t('Elapsed Time'),
            key: 'sys/duration_ms',
            value: typeof job?.duration === 'string' ? '-' : durationToStr(job?.duration as any),
        },
        {
            label: t('Created'),
            key: 'sys/created_time',
            value: job?.createdTime && job?.createdTime > 0 && formatTimestampDateTime(job?.createdTime),
        },
        {
            label: t('End Time'),
            key: 'sys/finished_time',
            value: job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-',
        },
        {
            label: t('Status'),
            key: 'sys/job_status',
            value: job?.jobStatus && <JobStatus key='jobStatus' status={job.jobStatus as any} />,
        },
        {
            key: 'sys/runtime_name',
            value: job?.runtime?.name,
        },
        {
            key: 'sys/runtime_uri',
            value: runtimeLink,
        },
        {
            key: 'sys/runtime_version',
            value: job?.runtime?.version?.name,
        },
        {
            key: 'sys/runtime_version_id',
            value: job?.runtime?.version?.id,
        },
        {
            key: 'sys/runtime_version_alias',
            value: <Alias alias={job?.runtime?.version?.alias} />,
        },
        {
            key: 'sys/model_version_alias',
            value: <Alias alias={job?.model?.version?.alias} />,
        },
        {
            key: 'sys/step_spec',
            value: (
                <div className='markdown-body'>
                    <pre>{job?.stepSpec}</pre>
                </div>
            ),
        },
        {
            key: 'sys/name',
            value: job?.jobName,
        },
        {
            key: 'sys/owner_id',
            value: job?.owner?.id,
        },
        {
            key: 'sys/owner_name',
            value: job?.owner?.name,
        },
        {
            key: 'sys/project_id',
            value: projectId,
        },
        {
            key: 'sys/dataset_uris',
            value: datasetUris,
        },
    ].sort((a, b) => {
        return a?.key?.localeCompare(b?.key)
    })

    return (
        <div className='flex-column overflow-auto'>
            {items.map((v) => (
                <div
                    key={v?.label}
                    style={{
                        display: 'flex',
                        gap: '20px',
                        borderBottom: '1px solid #EEF1F6',
                        lineHeight: '44px',
                        flexWrap: 'nowrap',
                        fontSize: '14px',
                        paddingLeft: '12px',
                    }}
                >
                    <div className='basis-170px overflow-hidden text-ellipsis flex-shrink-0 color-[rgba(2,16,43,0.60)]'>
                        {v?.key}
                    </div>
                    <div className='py-13px lh-18px'>{v?.value}</div>
                </div>
            ))}
        </div>
    )
}
