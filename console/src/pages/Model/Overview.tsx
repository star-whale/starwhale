import React from 'react'
import Table from '@/components/Table/index'
import useTranslation from '@/hooks/useTranslation'
import { useModel, useModelLoading } from '@model/hooks/useModel'
import Card from '@/components/Card'
import { IModelFileSchema } from '@model/schemas/model'

export default function ModelOverview() {
    const { model } = useModel()
    const { modelLoading } = useModelLoading()

    const [t] = useTranslation()

    const modelName = model?.name ?? ''

    return (
        <Card title={`${t('sth name', [t('Model')])}: ${modelName}`}>
            <Table
                isLoading={modelLoading}
                columns={[t('File'), t('Size')]}
                data={model?.files?.map((file: IModelFileSchema) => [file?.name, file?.size]) ?? []}
            />
        </Card>
    )
}
