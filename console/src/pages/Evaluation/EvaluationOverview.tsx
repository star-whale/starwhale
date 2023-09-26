import JobStatus from '@/domain/job/components/JobStatus'
import React from 'react'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { isSearchColumns, tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { useQueryDatastore } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import { QueryTableRequest } from '@starwhale/core/datastore'
import { useParams } from 'react-router-dom'
import { Text } from '@starwhale/ui/Text'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'

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

    return (
        <div
            className='flex-column '
            style={{
                fontSize: '14px',
                gridTemplateColumns: 'minmax(160px, max-content) 1fr',
                display: 'grid',
                overflow: 'auto',
                gridTemplateRows: 'repeat(100,44px)',
            }}
        >
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
                            default:
                                break
                        }

                        return (
                            <React.Fragment key={label}>
                                <div
                                    style={{
                                        color: 'rgba(2,16,43,0.60)',
                                        borderBottom: '1px solid #EEF1F6',
                                        display: 'flex',
                                        alignItems: 'center',
                                    }}
                                >
                                    {label}
                                </div>
                                <div
                                    className='line-clamp'
                                    style={{
                                        borderBottom: '1px solid #EEF1F6',
                                        paddingLeft: '20px',
                                        display: 'flex',
                                        alignItems: 'center',
                                    }}
                                >
                                    <Text maxWidth='1000px' tooltip={<pre>{value}</pre>}>
                                        {value}
                                    </Text>
                                </div>
                            </React.Fragment>
                        )
                    })}
        </div>
    )
}

export default EvaluationOverview
