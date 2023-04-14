import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useModelVersion } from '../../domain/model/hooks/useModelVersion'
import { MonoText } from '@/components/Text'

export default function ModelVersionOverview() {
    const { modelVersion } = useModelVersion()

    const [t] = useTranslation()
    const items = [
        {
            label: t('sth name', [t('Model')]),
            value: modelVersion?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: <MonoText>{modelVersion?.versionName ?? '-'} </MonoText>,
        },
        {
            label: t('Aliases'),
            value: modelVersion?.versionAlias ?? '-',
        },
        {
            label: t('Created At'),
            value: modelVersion?.createdTime && formatTimestampDateTime(modelVersion.createdTime),
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
