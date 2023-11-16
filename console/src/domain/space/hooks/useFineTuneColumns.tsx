import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { ExtendButton, VersionText, MonoText } from '@starwhale/ui'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { IDatasetVo, IFineTuneVo, IUserVo } from '@/api'
import User from '@/domain/user/components/User'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'
import { CustomColumn, StringColumn } from '@starwhale/ui/base/data-table'
import type { SharedColumnOptionsT, ColumnT } from '@starwhale/ui/base/data-table/types'
import { JobStatusType } from '@/domain/job/schemas/job'
import _ from 'lodash'

type DataT = IFineTuneVo

const shared = {
    isRenderRawCell: true,
}

const datasetsToStr = (datasets: IDatasetVo[]) => {
    return datasets?.flatMap((dataset) => [dataset.name, dataset.version.alias]).join(',') ?? ''
}

function IDColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
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
        mapDataToValue: (data: DataT) => data.job?.jobStatus as JobStatusType,
        renderCell: ({ value: jobStatus }) => <JobStatus status={jobStatus as any} />,
        ...shared,
        ...options,
    })
}
function DateTimeColumn(options: SharedColumnOptionsT<number | undefined>): ColumnT<number | undefined, any> {
    return CustomColumn({
        mapDataToValue: (data: DataT) => data.job?.createdTime,
        renderCell: ({ value: timestamp }) => timestamp && timestamp > 0 && formatTimestampDateTime(timestamp),
        ...shared,
        ...options,
    })
}
function DurationColumn(options: SharedColumnOptionsT<number | undefined>): ColumnT<number | undefined, any> {
    return CustomColumn({
        mapDataToValue: (data: DataT) => data.job?.duration,
        renderCell: ({ value: duration }) => duration && duration > 0 && durationToStr(duration),
        ...shared,
        ...options,
    })
}
function ResourcePoolColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        mapDataToValue: (data: DataT) => data.job?.resourcePool,
        ...shared,
        ...options,
    })
}
function ModelColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        mapDataToValue: (data: DataT) => data.job?.model?.name,
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
function AliasColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return CustomColumn({
        mapDataToValue: (data: DataT) => data.job?.model?.version.alias,
        renderCell: ({ value: alias }) => <Alias alias={alias} />,
        ...shared,
        ...options,
    })
}
function DatasetColumn(options: SharedColumnOptionsT<string>): ColumnT<string, any> {
    return StringColumn({
        mapDataToValue: (data: DataT) => datasetsToStr(data.job?.datasetList ?? []),
        ...shared,
        ...options,
    })
}

// VersionColumn({
//     title: t('ft.job.output_draft_model_version'),
//     key: t('ft.job.output_draft_model_version'),
//     mapDataToValue: (data: DataT) => data.targetModel?.version.name,
// }),

function useFineTuneColumns() {
    const [t] = useTranslation()

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
            mapDataToValue: (data: DataT) => datasetsToStr(data.trainDatasets ?? []),
        }),
        DatasetColumn({
            title: t('ft.validation_dataset.name'),
            key: 'evalDatasets',
            mapDataToValue: (data: DataT) => datasetsToStr(data.evalDatasets ?? []),
        }),
        ModelColumn({
            title: t('ft.job.output_model_name'),
            key: 'targetModelName',
            mapDataToValue: (data: DataT) => data.targetModel?.name,
            renderCell: ({ value: alias, data }) => (
                <>
                    {data?.job?.targetModel?.version.draft ? 'draft' : 'released'}
                    <Alias alias={alias} />
                </>
            ),
        }),
        AliasColumn({
            title: t('ft.job.output_model_version_alias'),
            key: 'targetModelVersionAlias',
            mapDataToValue: (data: DataT) => data.targetModel?.version.alias,
        }),
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
        JobStatusColumn({ title: t('Status'), key: 'status' }),
    ]
    const columnMap = _.keyBy(columns, 'key')

    const renderCell = (row) => (key) => {
        const { renderCell: RenderCell, mapDataToValue } = columnMap[key] ?? {}
        if (!RenderCell || !mapDataToValue) return null
        // @ts-ignore
        return <RenderCell value={mapDataToValue(row)} data={row} />
    }

    return { columns, columnMap, renderCell }
}

export { useFineTuneColumns }
export default useFineTuneColumns
