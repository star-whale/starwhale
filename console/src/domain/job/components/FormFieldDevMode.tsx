import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { ICreateJobFormSchema } from '../schemas/job'
import generatePassword from '@/utils/passwordGenerator'
import { EventEmitter } from 'ahooks/lib/useEventEmitter'
import { WithCurrentAuth } from '@/api/WithAuth'
import { Toggle } from '@starwhale/ui/Select'
import CopyToClipboard from 'react-copy-to-clipboard'
import Input from '@starwhale/ui/Input'
import IconFont from '@starwhale/ui/IconFont'
import { toaster } from 'baseui/toast'

function FormFieldDevMode({
    form,
    FormItem,
    // eslint-disable-next-line
    EventEmitter,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    EventEmitter: EventEmitter<any>
}) {
    const [t] = useTranslation()

    EventEmitter.useSubscription(({ changes: _changes }) => {
        if ('devMode' in _changes && _changes.devMode) {
            form.setFieldsValue({
                devPassword: generatePassword(),
            })
        }
    })

    return (
        <WithCurrentAuth id='job-dev'>
            {form.getFieldValue('devMode') && <p style={{ marginBottom: '10px' }}>{t('job.debug.notice')}</p>}
            <div style={{ width: '660px', marginBottom: '20px', display: 'flex', gap: '40px' }}>
                <FormItem label={t('job.debug.mode')} name='devMode'>
                    <Toggle />
                </FormItem>
                {form.getFieldValue('devMode') && (
                    <FormItem label={t('job.debug.password')} name='devPassword' required>
                        <Input
                            endEnhancer={
                                <CopyToClipboard
                                    text={form.getFieldValue('devPassword') as string}
                                    onCopy={() => {
                                        toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                                    }}
                                >
                                    <span style={{ cursor: 'pointer' }}>
                                        <IconFont type='overview' />
                                    </span>
                                </CopyToClipboard>
                            }
                        />
                    </FormItem>
                )}
            </div>
        </WithCurrentAuth>
    )
}

export default FormFieldDevMode
