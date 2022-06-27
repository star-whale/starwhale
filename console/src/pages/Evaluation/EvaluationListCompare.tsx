import React, { useMemo } from 'react'
import { usePage } from '@/hooks/usePage'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import Table from '@/components/Table/TableTyped'
import { useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { CustomColumn, StringColumn } from '@/components/data-table'
import { fetchJobResult } from '@/domain/job/services/job'
import { useQueries } from 'react-query'
import { IEvaluationAttributeValue } from '@/domain/evaluation/schemas/evaluation'

export default function EvaluationListCompare({
    rows = [],
    attrs = [],
}: {
    attrs: IEvaluationAttributeValue[]
    rows: any[]
}) {
    const [t] = useTranslation()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const evaluationsInfo = useFetchJobs(projectId, page)

    const results = useQueries(
        rows.map((row: any) => ({
            queryKey: `fetchJobResult:${projectId}:${row.id}`,
            queryFn: () => fetchJobResult(projectId, row.id),
            refetchOnWindowFocus: false,
        }))
    )

    console.log(results)

    const $columns = useMemo(
        () => [
            StringColumn({
                key: 'attrs',
                title: '',
                pin: 'LEFT',
                minWidth: 200,
                maxWidth: 200,
                mapDataToValue: (item: any) => item.title,
            }),
            ...rows.map((row: any, index) =>
                CustomColumn({
                    minWidth: 200,
                    key: String(row.id),
                    title: `${row.modelName}-${row.id}`,
                    // @ts-ignore
                    renderCell: (props: any) => {
                        const data = props.value || {}
                        return (
                            <div
                                title={data?.values?.[index]}
                                style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                            >
                                {data?.values?.[index] || ''}
                            </div>
                        )
                    },
                    mapDataToValue: (item: any) => item,
                })
            ),
        ],
        [rows]
    )

    const $rows = useMemo(
        () => [
            {
                key: 'evaluationId',
                title: t('Evaluation ID'),
                values: rows.map((data: any) => data.uuid),
            },
            {
                key: 'model',
                title: t('sth name', [t('Model')]),
                values: rows.map((data: any) => data.modelName),
            },
            {
                key: 'version',
                title: t('Version'),
                values: rows.map((data: any) => data.modelVersion),
            },
            {
                key: 'owner',
                title: t('Owner'),
                values: rows.map((data: any) => data.owner),
            },
            {
                key: 'createtime',
                title: t('Created time'),
                values: rows.map((data: any) => data.createdTime && formatTimestampDateTime(data.createdTime)),
            },
            {
                key: 'runtime',
                title: t('Run time'),
                values: rows.map((data: any) =>
                    typeof data.duration === 'string' ? '-' : durationToStr(data.duration)
                ),
            },
            {
                key: 'endtime',
                title: t('End time'),
                values: rows.map((data: any) => (data.stopTime > 0 ? formatTimestampDateTime(data.stopTime) : '-')),
            },
            // {
            //     key: 'status',
            //     title: t('Status'),
            //     values: rows.map((data: any) => data.jobStatus),
            // },
        ],
        [t, rows]
    )

    const $rowWithAttrs = useMemo(() => {
        const rowWithAttrs = [...$rows]

        attrs.map((attr, index) => {
            if (!attr.name.startsWith('summary/')) {
                return
            }

            const name = attr.name.split('/').slice(1).join('/')

            rowWithAttrs.push({
                key: attr.name,
                title: name,
                values: rows.map((data: any) => data?.attributes[index]?.value),
            })
        })

        return rowWithAttrs
    }, [$rows, attrs])

    return (
        <>
            <Table
                id='compare'
                isLoading={evaluationsInfo.isLoading}
                columns={$columns}
                // @ts-ignore
                data={$rowWithAttrs}
            />
        </>
    )
}
