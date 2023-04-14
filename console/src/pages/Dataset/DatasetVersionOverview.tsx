import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useDatasetVersion } from '../../domain/dataset/hooks/useDatasetVersion'
import { Toggle } from '@starwhale/ui'
import { fetchDatasetVersion, updateDatasetVersionShared } from '@/domain/dataset/services/datasetVersion'
import { toaster } from 'baseui/toast'
import { useParams } from 'react-router-dom'
import Shared from '@/components/Shared'
import { Alias } from '@/components/Alias'
import { MonoText } from '@/components/Text'

export default function DatasetVersionOverview() {
    const { projectId, datasetId, datasetVersionId } = useParams<{
        projectId: string
        datasetId: string
        datasetVersionId: string
    }>()
    const { datasetVersion: dataset, setDatasetVersion } = useDatasetVersion()

    const [t] = useTranslation()

    const items = [
        {
            label: t('sth name', [t('Dataset')]),
            value: dataset?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: <MonoText>{dataset?.versionName ?? '-'} </MonoText>,
        },
        {
            label: t('Aliases'),
            value: <Alias alias={dataset?.versionAlias} />,
        },
        {
            label: t('dataset.overview.shared'),
            value: (
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        height: '100%',
                        gap: '4px',
                    }}
                >
                    <Shared shared={dataset?.shared} isTextShow />
                    <Toggle
                        value={dataset?.shared === 1}
                        onChange={async (v) => {
                            try {
                                await updateDatasetVersionShared(projectId, datasetId, datasetVersionId, v)
                                const data = await fetchDatasetVersion(projectId, datasetId, datasetVersionId)
                                setDatasetVersion(data)
                                toaster.positive(t('dataset.overview.shared.success'))
                            } catch (e) {
                                toaster.negative(t('dataset.overview.shared.fail'))
                            }
                        }}
                    />
                </div>
            ),
        },
        {
            label: t('Created At'),
            value: dataset?.createdTime && formatTimestampDateTime(dataset.createdTime),
        },
    ]

    return (
        <div className='flex-column'>
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
                    <div
                        style={{
                            flexBasis: '110px',
                            color: 'rgba(2,16,43,0.60)',
                        }}
                    >
                        {v?.label}:
                    </div>
                    <div style={{ height: '100%' }}> {v?.value}</div>
                </div>
            ))}
        </div>
    )
}
