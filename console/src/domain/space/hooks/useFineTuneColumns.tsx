import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FilterString, FilterDatetime, FilterNumberical, ValueT, Operators } from '@starwhale/ui'
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

function IDColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        // @ts-ignore
        buildFilters: FilterString,
        mapDataToValue: (data: DataT) => String(data.id),
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

// function VersionColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
//     return CustomColumn({
//         mapDataToValue: (data: DataT) => data.job?.model?.version.name,
//         renderCell: ({ value: version }) => <VersionText version={version} />,
//         ...options,
//     })
// }
// VersionColumn({
//     title: t('ft.job.output_draft_model_version'),
//     key: t('ft.job.output_draft_model_version'),
//     mapDataToValue: (data: DataT) => data.targetModel?.version.name,
// }),

function useFineTuneColumns({ data: _data = {} }: { data?: IPageInfoFineTuneVo } = {}) {
    const [t] = useTranslation()
    const [queries, setQueries] = React.useState<ValueT[]>([])

    const { list = [] } = _data

    const columns = [
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
    ]
    const columnMap = _.keyBy(columns, 'key')

    const fields = columns.filter((v) => v.buildFilters)

    const fieldOptions = fields.map(({ key, title }) => {
        return {
            id: key,
            type: key,
            label: title,
        }
    })

    const renderCell = (row) => (key) => {
        const { renderCell: RenderCell, mapDataToValue } = columnMap[key] ?? {}
        if (!RenderCell || !mapDataToValue) return null
        // @ts-ignore
        return <RenderCell value={mapDataToValue(row)} data={row} />
    }

    const getFilters = (key = 'id') => {
        const { mapDataToValue, buildFilters } = columnMap[key] ?? {}
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
    }

    const $listFiltered = React.useMemo(() => {
        const set = new Set(list.map((__, idx) => idx))
        Array.from(queries || new Set(), (f) => f).forEach(({ property, op, value }: any) => {
            const filterFn = Operators[op]?.buildFilter?.({ value } as any)
            const { mapDataToValue } = columnMap[property] ?? {}

            if (!mapDataToValue) return

            Array.from(set).forEach((idx) => {
                if (filterFn && !filterFn(mapDataToValue(list[idx]))) {
                    set.delete(idx)
                }
            })
        })

        return [...set].map((idx) => list[idx])
    }, [list, queries, columnMap])

    return { columns, columnMap, renderCell, getFilters, list: $listFiltered, queries, setQueries }
}

export { useFineTuneColumns }
export default useFineTuneColumns
