import React from 'react'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import { IDatasetFileSchema } from '@dataset/schemas/dataset'

export default function DatasetVersionFiles() {
    const { dataset } = useDataset()
    const { datasetLoading } = useDatasetLoading()

    const [t] = useTranslation()

    return (
        <Table
            isLoading={datasetLoading}
            columns={[t('File'), t('Size')]}
            data={dataset?.files?.map((file: IDatasetFileSchema) => [file?.name, file?.size]) ?? []}
        />
    )
}
