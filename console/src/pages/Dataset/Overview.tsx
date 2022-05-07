import React from 'react'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import Card from '@/components/Card'
import { IDatasetFileSchema } from '@dataset/schemas/dataset'

export default function DatasetOverview() {
    const { dataset } = useDataset()
    const { datasetLoading } = useDatasetLoading()

    const [t] = useTranslation()

    const datasetName = dataset?.name ?? ''

    return (
        <Card title={`${t('sth name', [t('Dataset')])}: ${datasetName}`}>
            <Table
                isLoading={datasetLoading}
                columns={[t('File'), t('Size')]}
                data={dataset?.files?.map((file: IDatasetFileSchema) => [file?.name, file?.size]) ?? []}
            />
        </Card>
    )
}
