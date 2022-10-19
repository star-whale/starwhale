import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useDatasetVersion } from '../../domain/dataset/hooks/useDatasetVersion'

export default function DatasetVersionOverview() {
    const { datasetVersion: dataset } = useDatasetVersion()

    const [t] = useTranslation()

    const items = [
        // {
        //     label: t('Version ID'),
        //     value: dataset?.id ?? '-',
        // },
        {
            label: t('sth name', [t('Dataset')]),
            value: dataset?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: dataset?.versionName ?? '-',
        },
        {
            label: t('Aliases'),
            value: dataset?.versionAlias ?? '-',
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
                    <div> {v?.value}</div>
                </div>
            ))}
        </div>
    )
}
