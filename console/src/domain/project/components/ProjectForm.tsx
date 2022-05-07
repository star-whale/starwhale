import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import { ICreateProjectSchema, IProjectSchema } from '../schemas/project'

const { Form, FormItem } = createForm<ICreateProjectSchema>()

export interface IProjectFormProps {
    project?: IProjectSchema
    onSubmit: (data: ICreateProjectSchema) => Promise<void>
}

export default function ProjectForm({ project, onSubmit }: IProjectFormProps) {
    const [values, setValues] = useState<ICreateProjectSchema | undefined>({
        projectName: '',
    })

    useEffect(() => {
        if (!project) {
            return
        }
        setValues({
            projectName: project.name,
        })
    }, [project])

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
    }, [])

    const handleFinish = useCallback(
        async (values_) => {
            setLoading(true)
            try {
                await onSubmit(values_)
            } finally {
                setLoading(false)
            }
        },
        [onSubmit]
    )

    const [t] = useTranslation()

    return (
        <Form initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <FormItem name='projectName' label={t('sth name', [t('Project')])}>
                <Input disabled={project !== undefined ? true : undefined} />
            </FormItem>
            <FormItem>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button isLoading={loading} disabled={!isModified(project, values)}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}
