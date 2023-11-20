import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FilterString, FilterDatetime, FilterNumberical, ValueT, Operators, VersionText, Text } from '@starwhale/ui'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { IDatasetVo, IFineTuneVo, IPageInfoFineTuneVo, IUserVo } from '@/api'
import User from '@/domain/user/components/User'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'
import { CustomColumn, StringColumn } from '@starwhale/ui/base/data-table'
import type { SharedColumnOptionsT, ColumnT } from '@starwhale/ui/base/data-table/types'
import { JobStatusType } from '@/domain/job/schemas/job'
import _ from 'lodash'
import StatusTag from '@/components/Tag/StatusTag'
import { useEventCallback } from '@starwhale/core'
import { useParams } from 'react-router'

type DataT = IFineTuneVo

const shared = {
    isRenderRawCell: true,
}

const datasetsToStr = (datasets: IDatasetVo[]) => {
    return datasets?.flatMap((dataset) => [dataset.name, dataset.version.alias]).join(',') ?? ''
}

const datasetsToCell = (datasets: IDatasetVo[]) => {
    const tmp = datasets?.map((dataset, index) => (
        <div key={index} className='items-center px-8px py-5px rounded-4px flex gap-4px bg-[#EEF1F6] lh-none'>
            {dataset.name}
            <Alias alias={dataset.version.alias} />
        </div>
    ))
    if (!datasets || datasets.length === 0) return null

    return <div className='f-l-c gap-3px h-24px w-auto'>{tmp}</div>
}

function RawColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        // @ts-ignore
        buildFilters: FilterString,
        ...shared,
        ...options,
    })
}

function IDColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        // @ts-ignore
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => String(data?.id),
        ...shared,
        ...options,
    })
}
function OwnerColumn(options: SharedColumnOptionsT<IUserVo>): ColumnT<IUserVo, any> {
    return CustomColumn({
        mapDataToValue: (data: DataT) => data.job?.owner,
        renderCell: ({ value: user }) => <User user={user} />,
        ...shared,
        ...options,
    })
}
function JobStatusColumn(options: SharedColumnOptionsT<JobStatusType>): ColumnT<JobStatusType, any> {
    return CustomColumn({
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => data.job?.jobStatus as JobStatusType,
        renderCell: ({ value: jobStatus }) => <JobStatus status={jobStatus as any} />,
        ...shared,
        ...options,
    })
}
function DateTimeColumn(options: SharedColumnOptionsT<number | undefined>): ColumnT<number | undefined, any> {
    return CustomColumn({
        buildFilters: FilterDatetime,
        mapDataToValue: (data: DataT) => data.job?.createdTime,
        renderCell: ({ value: timestamp }) => timestamp && timestamp > 0 && formatTimestampDateTime(timestamp),
        ...shared,
        ...options,
    })
}
function DurationColumn(options: SharedColumnOptionsT<number | undefined>): ColumnT<number | undefined, any> {
    return CustomColumn({
        buildFilters: FilterNumberical,
        mapDataToValue: (data: DataT) => data.job?.duration,
        renderCell: ({ value: duration }) => duration && duration > 0 && durationToStr(duration),
        ...shared,
        ...options,
    })
}
function ResourcePoolColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        // @ts-ignore
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => data.job?.resourcePool,
        ...shared,
        ...options,
    })
}
function ModelColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        // @ts-ignore
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => data.job?.model?.name,
        ...shared,
        ...options,
    })
}

function AliasColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return CustomColumn({
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => data.job?.model?.version.alias,
        renderCell: ({ value: alias }) => <Alias alias={alias} />,
        ...shared,
        ...options,
    })
}
function DatasetColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        // @ts-ignore
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => datasetsToStr(data.job?.datasetList ?? []),
        ...shared,
        ...options,
    })
}

function VersionColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return CustomColumn({
        mapDataToValue: (data: DataT) => data.job?.model?.version.name,
        renderCell: ({ value: version }) => <VersionText version={version} />,
        ...options,
    })
}

const LIST_COLUMNS_KEYS = [
    'id',
    'baseModelName',
    'baseModelVersionAlias',
    'trainDatasets',
    'validationDatasets',
    'targetModelName',
    'targetModelVersionAlias',
    'status',
    'resourcePool',
    'owner',
    'createdTime',
    'stopTime',
    'duration',
]

const OVERVIEW_COLUMNS_KEYS = [
    'id',
    'status',
    'createdTime',
    'stopTime',
    'duration',
    'owner',
    'resourcePool',
    'targetModelName',
    'targetModelVersionAlias',
    'linkedTargetModelVersion',
    'baseModelName',
    'baseModelVersionAlias',
    'linkedBaseModelVersion',
    'trainDatasets',
    'validationDatasets',
    'runtimeName',
    'runtimeVersionAlias',
    'linkedRuntimeVersion',
    'handler',
    'stepSpec',
]

function useFineTuneColumns({
    data: _data = {},
    keys = LIST_COLUMNS_KEYS,
}: { data?: IPageInfoFineTuneVo; keys?: any[] } = {}) {
    const [t] = useTranslation()
    const [queries, setQueries] = React.useState<ValueT[]>([])
    const { projectId } = useParams<{ projectId: string }>()

    const { list = [] } = _data

    const $columns = React.useMemo(
        () => [
            IDColumn({ title: t('ft.id'), key: 'id' }),
            ModelColumn({
                title: t('ft.base.model.name'),
                key: 'baseModelName',
                mapDataToValue: (data: DataT) => data.job?.model?.name,
            }),
            AliasColumn({
                title: t('ft.base.model.version_alias'),
                key: 'baseModelVersionAlias',
                mapDataToValue: (data: DataT) => data.job?.model?.version.alias,
            }),
            DatasetColumn({
                title: t('ft.training_dataset.name'),
                key: 'trainDatasets',
                mapDataToValue: (data: DataT) => datasetsToStr(data?.trainDatasets ?? []),
                renderCell: ({ data }) => datasetsToCell(data?.trainDatasets ?? []),
            }),
            DatasetColumn({
                title: t('ft.validation_dataset.name'),
                key: 'validationDatasets',
                mapDataToValue: (data: DataT) => datasetsToStr(data?.validationDatasets ?? []),
                renderCell: ({ data }) => datasetsToCell(data?.validationDatasets ?? []),
            }),
            ModelColumn({
                title: t('ft.job.output_model_name'),
                key: 'targetModelName',
                mapDataToValue: (data: DataT) => data.targetModel?.name,
                renderCell: ({ value: name }) => <Text>{name}</Text>,
            }),
            AliasColumn({
                title: t('ft.job.output_model_version_alias'),
                key: 'targetModelVersionAlias',
                mapDataToValue: (data: DataT) => data.targetModel?.version.alias,
                renderCell: ({ value: alias, data }) => (
                    <div className='flex gap-2px lh-none'>
                        {data?.targetModel?.version.draft === true && (
                            <StatusTag>{t('ft.job.model.release.mode.draft')}</StatusTag>
                        )}
                        {data?.targetModel?.version.draft === false && (
                            <StatusTag kind='positive'>{t('ft.job.model.release.mode.released')}</StatusTag>
                        )}
                        <Alias alias={alias} />
                    </div>
                ),
            }),
            VersionColumn({
                title: t('ft.job.output_model_version'),
                key: 'targetModelVersion',
                mapDataToValue: (data: DataT) => data.targetModel?.version.name,
            }),
            JobStatusColumn({ title: t('Status'), key: 'status' }),
            ResourcePoolColumn({ title: t('Resource Pool'), key: 'resourcePool' }),
            OwnerColumn({ title: t('Owner'), key: 'owner' }),
            DateTimeColumn({
                title: t('Created'),
                key: 'createdTime',
                mapDataToValue: (data: DataT) => data.job?.createdTime,
            }),
            DateTimeColumn({
                title: t('End Time'),
                key: 'stopTime',
                mapDataToValue: (data: DataT) => data.job?.stopTime,
            }),
            DurationColumn({
                title: t('Elapsed Time'),
                key: 'duration',
                mapDataToValue: (data: DataT) => data.job?.duration,
            }),
            // overview columns
            RawColumn({
                title: t('ft.runtime.name'),
                key: 'runtimeName',
                mapDataToValue: (data: DataT) => data.job?.runtime?.name,
            }),
            RawColumn({
                title: t('ft.runtime.version'),
                key: 'runtimeVersion',
                mapDataToValue: (data: DataT) => data.job?.runtime?.version?.name,
            }),
            AliasColumn({
                title: t('ft.runtime.version_alias'),
                key: 'runtimeVersionAlias',
                mapDataToValue: (data: DataT) => data.job?.runtime?.version?.alias,
            }),
            RawColumn({
                title: t('ft.handler'),
                key: 'handler',
                mapDataToValue: (data: DataT) => data.job?.jobName ?? '',
            }),
            RawColumn({
                title: t('ft.step_spec'),
                key: 'stepSpec',
                mapDataToValue: (data: DataT) => data.job?.stepSpec ?? '',
                renderCell: ({ value }) => (
                    <div className='markdown-body'>
                        <pre>{value}</pre>
                    </div>
                ),
            }),
            // linked base model name
            ModelColumn({
                title: t('ft.base.model.version'),
                key: 'linkedBaseModelVersion',
                mapDataToValue: (data: DataT) => data.job?.model?.version?.name,
                renderCell: ({ value: version, data }: { value: string; data: DataT }) => {
                    const { model } = data.job
                    const to = `/projects/${projectId}/models/${model?.id}/versions/${model?.version?.id}/overview`
                    return <VersionText content={version} version={version} to={to} />
                },
            }),
            // linked target model version
            VersionColumn({
                title: t('ft.job.output_model_version'),
                key: 'linkedTargetModelVersion',
                mapDataToValue: (data: DataT) => data.targetModel?.version.name,
                renderCell: ({ value: version, data }: { value: string; data: DataT }) => {
                    const { targetModel } = data
                    const to = `/projects/${projectId}/models/${targetModel?.id}/versions/${targetModel?.version?.id}/overview`
                    return <VersionText content={version} version={version} to={to} />
                },
            }),
            // linked runtime version
            VersionColumn({
                title: t('ft.runtime.version'),
                key: 'linkedRuntimeVersion',
                mapDataToValue: (data: DataT) => data.job?.runtime?.version?.name,
                renderCell: ({ value: version, data }: { value: string; data: DataT }) => {
                    const { runtime } = data.job
                    const to = `/projects/${projectId}/runtimes/${runtime?.id}/versions/${runtime?.version?.id}/overview`
                    return <VersionText content={version} version={version} to={to} />
                },
            }),
            // RawColumn({
            //     title: t('job.debug.mode'),
            //     key: 'stepSpec',
            //     mapDataToValue: (data: DataT) => data.job?. ?? '',
            //     renderCell: ({ value }) => ,
            // }),
        ],
        [t, projectId]
    )

    const columnMap = React.useMemo(() => _.keyBy($columns, 'key'), [$columns])

    const columns = React.useMemo(() => {
        return keys.map((key) => columnMap[key])
    }, [columnMap, keys])

    const fields = React.useMemo(() => columns.filter((v) => v.buildFilters), [columns])

    const fieldOptions = React.useMemo(
        () =>
            fields.map(({ key, title }) => {
                return {
                    id: key,
                    type: key,
                    label: title,
                }
            }),
        [fields]
    )

    const renderCell = useEventCallback((row) => (key) => {
        const { renderCell: RenderCell, mapDataToValue } = columnMap[key] ?? {}
        if (!RenderCell || !mapDataToValue || !row) return null
        // @ts-ignore
        return <RenderCell value={mapDataToValue(row)} data={row} />
    })

    const getFilters = useEventCallback((key = 'id') => {
        if (!columnMap[key]) return undefined
        const { mapDataToValue, buildFilters } = columnMap[key]
        if (!mapDataToValue || !buildFilters) return undefined
        const values = list.map(mapDataToValue as any).filter((v) => (_.isNumber(v) ? true : Boolean(v)))
        const valueHints = new Set(values)
        const valueOptions = [...valueHints].map((v) => ({
            id: v,
            type: v,
            label: v,
        }))
        return buildFilters({
            fieldOptions,
            valueOptions,
        })
    })

    const $listFiltered = React.useMemo(() => {
        const set = new Set(list.map((__, idx) => idx))
        Array.from(queries || new Set(), (f) => f).forEach(({ property, op, value }: any) => {
            const filterFn = Operators[op]?.buildFilter?.({ value } as any)
            const { mapDataToValue } = columnMap[property] ?? {}

            if (!mapDataToValue || !filterFn) return

            Array.from(set).forEach((idx) => {
                if (!filterFn(mapDataToValue(list[idx]))) {
                    set.delete(idx)
                }
            })
        })

        return [...set].map((idx) => list[idx])
    }, [list, queries, columnMap])

    return { columns, columnMap, renderCell, getFilters, list: $listFiltered, queries, setQueries }
}

export {
    useFineTuneColumns,
    LIST_COLUMNS_KEYS,
    OVERVIEW_COLUMNS_KEYS,
    datasetsToStr,
    datasetsToCell,
    IDColumn,
    OwnerColumn,
    JobStatusColumn,
    DateTimeColumn,
    DurationColumn,
    ResourcePoolColumn,
    ModelColumn,
    AliasColumn,
    DatasetColumn,
    VersionColumn,
}
export default useFineTuneColumns
