import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useRuntime } from '@/domain/runtime/hooks/useRuntime'

export default function RuntimeVersionOverview() {
    const { runtime } = useRuntime()

    const [t] = useTranslation()

    const items = [
        {
            label: t('Version ID'),
            value: runtime?.id ?? '-',
        },
        {
            label: t('sth name', [t('Runtime')]),
            value: runtime?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: runtime?.versionName ?? '-',
        },
        {
            label: t('Aliases'),
            value: runtime?.versionAlias ?? '-',
        },
        {
            label: t('Created At'),
            value: runtime?.createdTime && formatTimestampDateTime(runtime.createdTime),
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
