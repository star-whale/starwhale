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

    return (
        <>
            <Card
                outTitle={t('Files')}
                style={{
                    fontSize: '14px',
                    padding: '12px 0',
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
