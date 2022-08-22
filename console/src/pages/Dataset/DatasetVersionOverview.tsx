import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useDataset } from '@dataset/hooks/useDataset'
import { formatTimestampDateTime } from '@/utils/datetime'

export default function DatasetVersionOverview() {
    const { dataset } = useDataset()

    const [t] = useTranslation()

    const items = [
        {
            label: t('Version Full Name'),
            value: dataset?.name ?? '-',
        },
        {
            label: t('Aliases'),
            value: dataset?.versionTag ?? '-',
        },
        // {
        //     label: t('Size'),
        //     value: dataset?.name ?? '-',
        // },
        // {
        //     label: t('Owner'),
        //     value: job?.owner?.name ?? '-',
        // },
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
