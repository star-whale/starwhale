import React from 'react'
import Table from '@/components/Table/index'
import useTranslation from '@/hooks/useTranslation'
import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import Card from '@/components/Card'
import { IRuntimeFileSchema } from '@/domain/runtime/schemas/runtime'
import { formatTimestampDateTime } from '@/utils/datetime'

export default function RuntimeOverview() {
    const { runtime } = useRuntime()
    const { runtimeLoading } = useRuntimeLoading()

    const [t] = useTranslation()

    const items = [
        {
            label: t('Runtime Name'),
            value: runtime?.runtimeName ?? '',
        },
        {
            label: t('Version Name'),
            value: runtime?.versionName ?? '',
        },
        {
            label: t('Version Meta'),
            value: runtime?.versionMeta ?? '',
        },
        {
            label: t('Version Tag'),
            value: runtime?.versionTag ?? '',
        },
        {
            label: t('Created'),
            value: runtime?.createdTime && formatTimestampDateTime(runtime.createdTime),
        },
    ]

    const info = (
        <Card
            style={{
                fontSize: '14px',
                background: 'var(--color-brandBgSecondory4)',
                padding: '12px 20px',
                marginBottom: '10px',
            }}
            bodyStyle={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
                gap: '12px',
            }}
        >
            {items.map((v) => (
                <div key={v?.label} style={{ display: 'flex', gap: '12px' }}>
                    <div
                        style={{
                            background: 'var(--color-brandBgSecondory)',
                            lineHeight: '24px',
                            padding: '0 12px',
                            borderRadius: '4px',
                        }}
                    >
                        {v?.label}:
                    </div>
                    <div> {v?.value}</div>
                </div>
            ))}
        </Card>
    )

    return (
        <>
            {info}

            <Card
                outTitle={t('Files')}
                style={{
                    fontSize: '14px',
                    padding: '12px 20px',
                    marginBottom: '10px',
                }}
            >
                <Table
                    isLoading={runtimeLoading}
                    columns={[t('File'), t('Size')]}
                    data={runtime?.files?.map((file: IRuntimeFileSchema) => [file?.name, file?.size]) ?? []}
                />
            </Card>
        </>
    )
}
