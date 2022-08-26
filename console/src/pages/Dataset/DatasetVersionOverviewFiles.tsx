import React from 'react'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import { IDatasetFileSchema } from '@dataset/schemas/dataset'
import { useFetchDatasetList } from '@/domain/datastore/hooks/useFetchDatastore'
import { useParams } from 'react-router-dom'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export default function DatasetVersionFiles() {
    const { dataset } = useDataset()
    const { projectId } = useParams<{ projectId: string }>()
    const projectInfo = useFetchProject(projectId)

    const { datasetLoading } = useDatasetLoading()

    const tables = useFetchDatasetList(
        projectInfo.data?.name ?? '',
        dataset?.name ?? '',
        'mu2wmyjvgmztqnrtmftdgyjzgjrwonq'
    )

    console.log(projectInfo.data?.name, dataset?.name, dataset?.versionName, tables)

    const [t] = useTranslation()

    return (
        <Table
            isLoading={datasetLoading}
            columns={[t('File'), t('Size')]}
            data={dataset?.files?.map((file: IDatasetFileSchema) => [file?.name, file?.size]) ?? []}
        />
    )
}
