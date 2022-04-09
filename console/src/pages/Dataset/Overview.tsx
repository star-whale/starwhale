import React, { useCallback, useState } from 'react'
import { RiSurveyLine } from 'react-icons/ri'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import Card from '@/components/Card'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { ICreateDatasetSchema, IDatasetFileSchema } from '@dataset/schemas/dataset'
import { createDataset } from '@dataset/services/dataset'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import DatasetForm from '@dataset/components/DatasetForm'
import { useFetchDatasets } from '@dataset/hooks/useFetchDatasets'
import { usePage } from '@/hooks/usePage'

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
