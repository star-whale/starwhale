import JobStatus from '@/domain/job/components/JobStatus'
import React from 'react'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { isSearchColumns, tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { useQueryDatastore } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import { QueryTableRequest } from '@starwhale/core/datastore'
import { useParams } from 'react-router-dom'
import { Text } from '@starwhale/ui/Text'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { TextLink } from '@/components/Link'

function EvaluationOverview() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()

    const query = React.useMemo(
        () => ({
            tableName: tableNameOfSummary(projectId),
            start: 0,
            limit: 1,
            rawResult: true,
            ignoreNonExistingTable: true,
            filter: {
                operator: 'EQUAL',
                operands: [
                    {
                        intValue: jobId,
                    },
                    {
                        columnName: 'sys/id',
                    },
                ],
            },
        }),
        [jobId, projectId]
    )

    const info = useQueryDatastore(query as QueryTableRequest)

    const record: Record<string, any> | undefined = info?.data?.records?.[0]

    const datasetUri = record?.['sys/dataset_uris']?.split(',')
    const datasetLinkMap = record?.['sys/_dataset_uris']?.map((v: string, index: number) => {
        const str = v.replace('project', 'projects').replace('dataset', 'datasets').replace('version', 'versions')
        return (
            <TextLink key={v} to={`/${str}/overview`} baseStyle={{ maxWidth: 'none' }}>
                {datasetUri[index]}
            </TextLink>
        )
    })

    const runtimeUri = record?.['sys/runtime_uri']
    const runtimeTo = record?.['sys/_runtime_uri']
        .replace('project', 'projects')
        .replace('runtime', 'runtimes')
        .replace('version', 'versions')
    const runtimeLink = (
        <TextLink key={runtimeUri} to={`/${runtimeTo}/overview`} baseStyle={{ maxWidth: 'none' }}>
            {runtimeUri}
        </TextLink>
    )

    const modelUri = record?.['sys/model_uri']
    const modelTo = record?.['sys/_model_uri']
        .replace('project', 'projects')
        .replace('model', 'models')
        .replace('version', 'versions')
    const modelLink = (
        <TextLink key={modelUri} to={`/${modelTo}/overview`} baseStyle={{ maxWidth: 'none' }}>
            {modelUri}
        </TextLink>
    )

    // const modelTo = `/projects/${project?.id}/models/${job?.model?.id}/versions/${job?.model?.version?.id}/overview`
    // const modelLink = (
    //     <TextLink key={modelUri} to={modelTo} baseStyle={{ maxWidth: 'none' }}>
    //         {modelUri}
    //     </TextLink>
    // )

    return (
        <div className='flex-column'>
            {!record && (
                <BusyPlaceholder
                    type='notfound'
                    style={{
                        height: '148px',
                        minHeight: '148px',
                        overflow: 'hidden',
                    }}
                />
            )}
            {record &&
                Object.keys(record)
                    .sort((a, b) => {
                        if (a === 'id') return -1
                        return a.localeCompare(b)
                    })
                    .filter((label) => isSearchColumns(label) && typeof record[label] !== 'object')
                    .map((label) => {
                        let value = record[label]
                        switch (label) {
                            case 'sys/job_status':
                                value = <JobStatus status={record[label] as any} />
                                break
                            case 'sys/modified_time':
                            case 'sys/created_time':
                            case 'sys/finished_time':
                                value = Number(value) > 0 ? formatTimestampDateTime(value) : 0
                                break
                            case 'sys/duration_ms':
                                value = Number(value) > 0 ? durationToStr(value) : 0
                                break
                            case 'sys/step_spec':
                                value = (
                                    <div className='markdown-body'>
                                        <pre>{value}</pre>
                                    </div>
                                )
                                break
                            case 'sys/model_uri':
                                value = modelLink
                                break
                            case 'sys/runtime_uri':
                                value = runtimeLink
                                break
                            case 'sys/dataset_uris':
                                value = datasetLinkMap
                                break
                            default:
                                break
                        }

                        return (
                            <div
                                key={label}
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
                                <div className='basis-250px overflow-hidden text-ellipsis flex-shrink-0 color-[rgba(2,16,43,0.60)]'>
                                    <Text maxWidth='1000px' tooltip={<pre>{label}</pre>}>
                                        {label}
                                    </Text>{' '}
                                </div>
                                <div className='py-13px lh-18px'>{value}</div>
                            </div>
                        )
                    })}
        </div>
    )
}

export default EvaluationOverview
