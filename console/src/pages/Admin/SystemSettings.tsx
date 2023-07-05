import React, { useCallback, useState } from 'react'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE, KIND } from 'baseui/button'
import Editor from '@monaco-editor/react'
import yaml from 'js-yaml'
import { createUseStyles } from 'react-jss'
import { toaster } from 'baseui/toast'
import { useFetchSystemSetting } from '@/domain/setting/hooks/useSettings'
import { updateSystemSetting } from '@/domain/setting/services/system'

const { Form, FormItem, useForm } = createForm<any>({
    itemClassName: {
        label: 'flex',
    },
})

const useStyles = createUseStyles({
    form: {
        'display': 'flex',
        'flexDirection': 'column',
        'flex': 1,
        '& [data-baseweb="form-control-container"]': {
            flex: 1,
        },
        '& [class*=formItem]:nth-child(1)': {
            display: 'flex',
            flexDirection: 'column',
            flex: 1,
            minWidth: 0,
        },
    },
    buttonGroup: {
        display: 'flex',
        gap: 20,
        marginTop: 60,
    },
})

export default function SystemSettings() {
    const styles = useStyles()
    const [t] = useTranslation()
    const [form] = useForm()
    const systemSetting = useFetchSystemSetting()
    const [loading, setLoading] = useState(false)
    const [values, setValues] = React.useState<any>(null)

    React.useEffect(() => {
        if (systemSetting.isSuccess) {
            if (typeof systemSetting.data === 'string')
                form.setFieldsValue({
                    setting: systemSetting.data,
                })
        }
    }, [systemSetting.isSuccess, systemSetting.data, form])

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
    }, [])

    const handleFinish = useCallback(
        async (values_: any) => {
            try {
                yaml.load(values_.setting)
            } catch (e) {
                toaster.negative(t('wrong yaml syntax'), { autoHideDuration: 1000 })
                throw e
            }
            setLoading(true)
            try {
                await updateSystemSetting(values_.setting)
                toaster.positive(t('Update Setting Success'), { autoHideDuration: 1000 })
            } finally {
                setLoading(false)
            }
        },
        [t]
    )

    return (
        <Form
            className={styles.form}
            form={form}
            initialValues={values}
            onFinish={handleFinish}
            onValuesChange={handleValuesChange}
        >
            <FormItem label='' name='setting' required>
                <Editor width='960px' defaultLanguage='yaml' theme='vs-dark' />
            </FormItem>
            <FormItem>
                <div className={styles.buttonGroup}>
                    <Button
                        size={SIZE.compact}
                        kind={KIND.secondary}
                        type='button'
                        onClick={() => {
                            form.setFieldsValue({
                                setting: systemSetting.data,
                            })
                        }}
                    >
                        {t('Reset')}
                    </Button>
                    <Button size={SIZE.compact} isLoading={loading}>
                        {t('Update')}
                    </Button>
                    <div style={{ flexGrow: 1 }} />
                </div>
            </FormItem>
        </Form>
    )
}
