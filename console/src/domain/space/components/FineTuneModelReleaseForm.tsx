import React, { useState } from 'react'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { createUseStyles } from 'react-jss'
import Input from '@starwhale/ui/Input'
import { useEventCallback } from '@starwhale/core'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { Radio, RadioGroup } from '@starwhale/ui'
import ModelSelector from '@/domain/model/components/ModelSelector'
import { useParams } from 'react-router-dom'
import { api } from '@/api'
import { toaster } from 'baseui/toast'

const { Form, FormItem } = createForm<IFormValueProps>()

interface IFormValueProps {
    mode: 'CREATE' | 'APPEND'
    nonExistingModelName?: string
    existingModelId?: string
}

const useStyles = createUseStyles({
    project: {},
    projectName: {
        display: 'flex',
        alignContent: 'stretch',
        alignItems: 'flex-start',
    },
})

export interface IFormProps {
    onSubmit: (data: IFormValueProps) => Promise<void>
}

const Mode = ({ value, onChange }: any) => {
    const [t] = useTranslation()
    return (
        <RadioGroup value={value} onChange={(e) => onChange?.(e.target.value)} align='vertical'>
            <Radio value='CREATE'>{t('ft.job.model.release.mode.create')}</Radio>
            <Radio value='APPEND'>{t('ft.job.model.release.mode.append')}</Radio>
        </RadioGroup>
    )
}
export default function FineTuneModelReleaseForm({ onSubmit }: IFormProps) {
    const [t] = useTranslation()
    const styles = useStyles()
    const { projectId } = useParams<{ projectId: any }>()

    const [values, setValues] = useState<IFormValueProps | undefined>({
        mode: 'APPEND',
    })

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useEventCallback((_changes, values_) => {
        setValues(values_)
    })

    const handleFinish = useEventCallback(async (values_) => {
        setLoading(true)
        try {
            await onSubmit({
                // ...data,
                ...values_,
            })
        } finally {
            setLoading(false)
        }
    })

    const { mode } = values || {}

    return (
        <Form
            className={styles.project}
            initialValues={values}
            onFinish={handleFinish}
            onValuesChange={handleValuesChange}
        >
            <div className='flex gap-12px'>
                <div className='font-14px lh-32px color-[#02102B]'>{t('ft.job.model.release.mode')}:</div>
                <div className='flex-col'>
                    <FormItem name='mode' required>
                        <Mode />
                    </FormItem>
                </div>
            </div>
            {mode === 'CREATE' && (
                <FormItem name='nonExistingModelName' label={t('sth name', [t('Model')])} required>
                    <Input />
                </FormItem>
            )}
            {mode === 'APPEND' && (
                <FormItem name='existingModelId' label={t('sth name', [t('Model')])} required>
                    <ModelSelector projectId={projectId} />
                </FormItem>
            )}
            <FormItem key='submit'>
                <div className='flex'>
                    <div className='flex-grow' />
                    <Button size='compact' isLoading={loading}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

export function FineTuneModelReleaseModal({ isOpen, setIsOpen, data, onRefresh }) {
    const [t] = useTranslation()
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const handleSubmit = useEventCallback(async (values) => {
        await api.releaseFt(projectId, spaceId, {
            ftId: data.id,
            nonExistingModelName: values.nonExistingModelName,
            existingModelId: values.existingModelId,
        })
        setIsOpen(false)
        toaster.positive(t('ft.job.model.release.succes'), { autoHideDuration: 2000 })
        onRefresh?.()
    })

    return (
        <Modal isOpen={isOpen} onClose={() => setIsOpen(false)} closeable animate autoFocus>
            <ModalHeader>{`${t('Model')} ${t('ft.job.model.release')}`}</ModalHeader>
            <ModalBody>
                <div className='bt-1px bb-1px pt-20px'>
                    <FineTuneModelReleaseForm onSubmit={handleSubmit} data={data} />
                </div>
            </ModalBody>
        </Modal>
    )
}
