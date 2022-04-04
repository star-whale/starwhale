import React, { useCallback, useState } from 'react'
import { RiSurveyLine } from 'react-icons/ri'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useModel, useModelLoading } from '@model/hooks/useModel'
import Card from '@/components/Card'
import User from '@/components/User'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { ICreateModelSchema, IModelFileSchema } from '@model/schemas/model'
import { createModel } from '@model/services/model'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import ModelForm from '@model/components/ModelForm'
import { useFetchModels } from '@model/hooks/useFetchModels'
import { usePage } from '@/hooks/usePage'

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
