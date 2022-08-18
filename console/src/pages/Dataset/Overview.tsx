import React, { useMemo } from 'react'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import Card from '@/components/Card'
import { IDatasetFileSchema } from '@dataset/schemas/dataset'
import { formatTimestampDateTime } from '@/utils/datetime'
import yaml from 'js-yaml'
import { JSONTree } from 'react-json-tree'

export default function DatasetOverview() {
    const { dataset } = useDataset()
    const { datasetLoading } = useDatasetLoading()

    const [t] = useTranslation()

    const items = [
        {
            label: t('Version Name'),
            value: dataset?.versionName ?? '',
        },
        {
            label: t('Version Tag'),
            value: dataset?.versionTag ?? '',
        },
        {
            label: t('Created'),
            value: dataset?.createdTime && formatTimestampDateTime(dataset.createdTime),
        },
    ]

    const jsonData = useMemo(() => {
        if (!dataset?.versionMeta) return {}
        return yaml.load(dataset?.versionMeta)
    }, [dataset?.versionMeta])

    const info = (
        <Card
            style={{
                fontSize: '16px',
                background: 'var(--color-brandBgSecondary4)',
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
                            // flexBasis: '130px',
                            background: 'var(--color-brandBgSecondary)',
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
                outTitle={t('Version Meta')}
                style={{
                    fontSize: '14px',
                    padding: '12px 20px',
                    marginBottom: '10px',
                    backgroundColor: 'rgb(0, 43, 54)',
                }}
            >
                <div style={{ padding: '10px', backgroundColor: 'rgb(0, 43, 54)' }}>
                    <JSONTree data={jsonData} hideRoot shouldExpandNode={() => true} />
                </div>
            </Card>
            <Card
                outTitle={t('Files')}
                style={{
                    fontSize: '14px',
                    padding: '12px 20px',
                    marginBottom: '10px',
                }}
            >
                <Table
                    isLoading={datasetLoading}
                    columns={[t('File'), t('Size')]}
                    data={dataset?.files?.map((file: IDatasetFileSchema) => [file?.name, file?.size]) ?? []}
                />
            </Card>
        </>
    )
}
