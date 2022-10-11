import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useRuntimeVersion } from '@/domain/runtime/hooks/useRuntimeVersion'

export default function RuntimeVersionOverview() {
    const { runtimeVersion } = useRuntimeVersion()

    const [t] = useTranslation()

    const items = [
        {
            label: t('sth name', [t('Runtime')]),
            value: runtimeVersion?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: runtimeVersion?.versionName ?? '-',
        },
        {
            label: t('Aliases'),
            value: runtimeVersion?.versionAlias ?? '-',
        },
        {
            label: t('Created At'),
            value: runtimeVersion?.createdTime && formatTimestampDateTime(runtimeVersion.createdTime),
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
