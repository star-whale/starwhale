import React from 'react'
import Table from '@/components/Table/index'
import useTranslation from '@/hooks/useTranslation'
import { useModel, useModelLoading } from '@model/hooks/useModel'
import Card from '@/components/Card'
import { IModelFileSchema } from '@model/schemas/model'
import { formatTimestampDateTime } from '@/utils/datetime'

export default function ModelOverview() {
    const { model } = useModel()
    const { modelLoading } = useModelLoading()

    const [t] = useTranslation()

    const items = [
        {
            label: t('Version Name'),
            value: model?.versionName ?? '',
        },
        {
            label: t('Version Meta'),
            value: model?.versionMeta ?? '',
        },
        {
            label: t('Version Tag'),
            value: model?.versionTag ?? '',
        },
        {
            label: t('Model ID'),
            value: model?.id ?? '',
        },
        {
            label: t('Created'),
            value: model?.createdTime && formatTimestampDateTime(model.createdTime),
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
                gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))',
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
                    isLoading={modelLoading}
                    columns={[t('File'), t('Size')]}
                    data={model?.files?.map((file: IModelFileSchema) => [file?.name, file?.size]) ?? []}
                />
            </Card>
        </>
    )
}
