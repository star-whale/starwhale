import React, { useState } from 'react'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import { Textarea } from 'baseui/textarea'
import { createUseStyles } from 'react-jss'
import Input from '@starwhale/ui/Input'
import { IFineTuneSpaceCreateRequest, IFineTuneSpaceVo } from '@/api'
import { useEventCallback } from '@starwhale/core'
import _ from 'lodash'

const { Form, FormItem } = createForm<IFineTuneSpaceCreateRequest>()

const useStyles = createUseStyles({
    project: {},
    projectName: {
        display: 'flex',
        alignContent: 'stretch',
        alignItems: 'flex-start',
    },
})

export interface IFormProps {
    data?: IFineTuneSpaceVo
    onSubmit: (data: IFineTuneSpaceVo) => Promise<void>
    label?: string
}

export default function SpaceForm({ data, label, onSubmit }: IFormProps) {
    const [t] = useTranslation()
    const styles = useStyles()

    const [values, setValues] = useState<IFineTuneSpaceVo | undefined>(data)

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useEventCallback((_changes, values_) => {
        setValues(values_)
    })

    const handleFinish = useEventCallback(async (values_) => {
        setLoading(true)
        try {
            await onSubmit({
                ...data,
                ...values_,
            })
        } finally {
            setLoading(false)
        }
    })

    return (
        <Form
            className={styles.project}
            initialValues={values}
            onFinish={handleFinish}
            onValuesChange={handleValuesChange}
        >
            <FormItem name='name' label={t('sth name', [label])} required>
                <Input size='compact' />
            </FormItem>
            <FormItem name='description' label={t('Description')}>
                <Textarea
                    overrides={{
                        // @ts-ignore
                        Root: {
                            style: {
                                borderTopWidth: '1px',
                                borderBottomWidth: '1px',
                                borderLeftWidth: '1px',
                                borderRightWidth: '1px',
                            },
                        },
                    }}
                />
            </FormItem>
            <FormItem key='submit'>
                <div className='flex'>
                    <div className='flex-grow' />
                    <Button
                        size='compact'
                        isLoading={loading}
                        disabled={
                            !isModified(_.pick(data, ['name', 'description']), _.pick(values, ['name', 'description']))
                        }
                    >
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}
