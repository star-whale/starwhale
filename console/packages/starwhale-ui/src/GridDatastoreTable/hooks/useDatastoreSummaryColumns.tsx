import { CustomColumn } from '../../base/data-table'
import { ColumnT } from '../../base/data-table/types'
import { RecordAttr } from '../recordAttrModel'
import { useDatastoreColumns } from './useDatastoreColumns'
import React, { useMemo } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IconLink, TextLink } from '@/components/Link'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import JobStatus from '@/domain/job/components/JobStatus'
import ModelTreeSelector from '@/domain/model/components/ModelTreeSelector'
import JobStatusSelector from '@/domain/job/components/JobStatusSelector'
import ModelSelector from '@/domain/model/components/ModelSelector'
import { FilterDatetime } from '@starwhale/ui/Search'
import { ColumnHintsDesc, ColumnSchemaDesc } from '@starwhale/core'
import IconFont from '@starwhale/ui/IconFont'

export function useDatastoreSummaryColumns(
    options: {
        // @ts-ignore
        projectId?: string
        fillWidth?: boolean
        parseLink?: (link: string) => (str: any) => string
        showPrivate?: boolean
        showLink?: boolean
        columnTypes?: ColumnSchemaDesc[]
        columnHints?: Record<string, ColumnHintsDesc>
        hasAction?: boolean
    } = {
        fillWidth: false,
        projectId: '',
        hasAction: false,
    }
): ColumnT[] {
    const [t] = useTranslation()
    const { projectId, hasAction } = options

    const $columns = useDatastoreColumns(options)

    const $columnsWithSpecColumns = useMemo(() => {
        const _tmp = $columns.map((column) => {
            if (column.key === 'sys/id')
                return CustomColumn<RecordAttr, any>({
                    ...column,
                    renderCell: ({ value: record }) => {
                        const id = record.value
                        if (!id) return <></>
                        return <TextLink to={`/projects/${projectId}/evaluations/${id}/results`}>{id}</TextLink>
                    },
                })
            if (column.key === 'sys/duration_ms')
                return CustomColumn<RecordAttr, any>({
                    ...column,
                    renderCell: ({ value }) => <p title={value.toString()}>{durationToStr(value.value)}</p>,
                })
            if (column.key === 'sys/dev_mode')
                return CustomColumn<RecordAttr, any>({
                    ...column,
                    renderCell: ({ value }) => (
                        <p title={value.toString()}>{value.value !== '0' ? t('yes') : t('no')}</p>
                    ),
                })
            if (column.key === 'sys/job_status')
                return CustomColumn<RecordAttr, any>({
                    ...column,
                    renderCell: ({ value }) => (
                        <div title={value.toString()}>
                            <JobStatus status={value.toString() as any} />
                        </div>
                    ),
                    filterable: true,
                    renderFilter: function RenderFilter() {
                        return <JobStatusSelector clearable multiple />
                    },
                })
            if (column.key?.endsWith('time')) {
                return CustomColumn<RecordAttr, any>({
                    ...column,
                    renderCell: ({ value }) => {
                        return (
                            <span className='line-clamp line-clamp-2' title={value.toString()}>
                                {Number(value.value) > 0 ? formatTimestampDateTime(value.value) : '-'}{' '}
                            </span>
                        )
                    },
                    getFilters: () => column.buildFilters?.(FilterDatetime),
                })
            }
            if (column.key === 'sys/model_name') {
                return {
                    ...column,
                    filterable: true,
                    renderFilter: function RenderFilter() {
                        if (!projectId) return <></>
                        return <ModelSelector projectId={projectId} clearable getId={(v) => v.name} />
                    },
                }
            }
            if (column.key === 'sys/model_version')
                return CustomColumn({
                    ...column,
                    filterable: true,
                    renderFilter: function RenderFilter() {
                        if (!projectId) return <></>
                        return (
                            <ModelTreeSelector
                                placeholder={t('model.selector.version.placeholder')}
                                projectId={projectId}
                                multiple
                                clearable
                                getId={(v: any) => v.versionName}
                            />
                        )
                    },
                })

            return { ...column }
        })

        return [
            ..._tmp,
            ...(hasAction
                ? [
                      CustomColumn<RecordAttr, any>({
                          ..._tmp[0],
                          key: 'action',
                          title: t('Action'),
                          pin: 'RIGHT',
                          columnable: false,
                          renderCell: ({ value: record }) => {
                              const id = record.record?.['sys/id']
                              if (!id) return <></>
                              return (
                                  <IconLink to={`/projects/${projectId}/jobs/${id}/tasks`}>
                                      <IconFont type='tasks' />
                                  </IconLink>
                              )
                          },
                      }),
                  ]
                : []),
        ]
    }, [$columns, projectId, t, hasAction])

    return $columnsWithSpecColumns
}
