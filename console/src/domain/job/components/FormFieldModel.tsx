import React, { useEffect } from 'react'
import { FormSelect, Toggle } from '@starwhale/ui/Select'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { useParams } from 'react-router-dom'
import { ICreateJobFormSchema } from '../schemas/job'
import ModelTreeSelector from '@/domain/model/components/ModelTreeSelector'
import { EventEmitter } from 'ahooks/lib/useEventEmitter'
import Editor from '@monaco-editor/react'
import { createUseStyles } from 'react-jss'
import yaml from 'js-yaml'
import { toaster } from 'baseui/toast'
import { StepSpec } from '@/domain/model/schemas/modelVersion'

const useStyles = createUseStyles({
    modelField: {
        display: 'grid',
        columnGap: 40,
        gridTemplateColumns: '660px 280px 280px',
        gridTemplateRows: 'minmax(0px, max-content)',
    },
})

function FormFieldModel({
    form,
    FormItem,
    eventEmitter,
    stepSource,
    setModelTree,
    fullStepSource,
    forceUpdate,
    autoFill,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    eventEmitter: EventEmitter<any>
    stepSource?: StepSpec[]
    setModelTree: (obj: any) => void
    fullStepSource?: StepSpec[]
    forceUpdate: () => void
    autoFill?: boolean
}) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()
    const styles = useStyles()

    eventEmitter.useSubscription(({ changes: _changes, values: values_ }) => {
        if ('modelVersionUrl' in _changes) {
            form.setFieldsValue({
                modelVersionHandler: '',
            })
        }

        if ('rawType' in _changes && !_changes.rawType) {
            try {
                yaml.load(values_.stepSpecOverWrites)
            } catch (e) {
                toaster.negative(t('wrong yaml syntax'), { autoHideDuration: 1000 })
                form.setFieldsValue({
                    rawType: true,
                })
            }
        }

        if ('modelVersionHandler' in _changes && fullStepSource) {
            form.setFieldsValue({
                stepSpecOverWrites: yaml.dump(
                    fullStepSource.filter((v: StepSpec) => v?.job_name === _changes.modelVersionHandler)
                ),
            })
        }
    })

    const modelVersionHandler = form.getFieldValue('modelVersionHandler')
    const _modelVersionUrl = form.getFieldValue('modelVersionUrl')
    const rawType = form.getFieldValue('rawType')

    useEffect(() => {
        if (!fullStepSource) return

        if (!modelVersionHandler) {
            form.setFieldsValue({
                modelVersionHandler: fullStepSource.find((v) => v)?.job_name ?? '',
            })
        }

        if (!autoFill) return

        if (modelVersionHandler) {
            form.setFieldsValue({
                stepSpecOverWrites: yaml.dump(
                    fullStepSource.filter((v: StepSpec) => v?.job_name === modelVersionHandler)
                ),
            })
        }
        forceUpdate()
    }, [form, autoFill, fullStepSource, modelVersionHandler, forceUpdate])

    return (
        <>
            <div className={styles.modelField}>
                <FormItem label={t('Model Version')} required name='modelVersionUrl'>
                    <ModelTreeSelector
                        projectId={projectId}
                        onDataChange={setModelTree}
                        // getId={(obj) => obj.versionName}
                    />
                </FormItem>
                {_modelVersionUrl && fullStepSource && (
                    <FormItem label={t('model.handler')} required name='modelVersionHandler'>
                        {/* @ts-ignore */}
                        <FormSelect
                            clearable={false}
                            options={
                                (Array.from(new Set(fullStepSource?.map((tmp) => tmp.job_name))).map((tmp) => {
                                    return {
                                        label: tmp,
                                        id: tmp,
                                    }
                                }) ?? []) as any
                            }
                        />
                    </FormItem>
                )}
                <FormItem label={t('Raw Type')} name='rawType'>
                    <Toggle />
                </FormItem>
            </div>
            <div style={{ paddingBottom: '0px' }}>
                {stepSource &&
                    stepSource?.length > 0 &&
                    !rawType &&
                    stepSource?.map((spec, i) => {
                        return (
                            <div key={[spec?.name, i].join('')}>
                                <div
                                    style={{
                                        display: 'flex',
                                        minWidth: '280px',
                                        lineHeight: '1',
                                        alignItems: 'stretch',
                                        gap: '20px',
                                        marginBottom: '10px',
                                    }}
                                >
                                    <div
                                        style={{
                                            padding: '5px 20px',
                                            minWidth: '280px',
                                            background: '#EEF1F6',
                                            borderRadius: '4px',
                                        }}
                                    >
                                        <span style={{ color: 'rgba(2,16,43,0.60)' }}>{t('Step')}:&nbsp;</span>
                                        <span>{spec?.name}</span>
                                        <div style={{ marginTop: '3px' }} />
                                        <span style={{ color: 'rgba(2,16,43,0.60)' }}>{t('Task Amount')}:&nbsp;</span>
                                        <span>{spec?.replicas}</span>
                                    </div>
                                    {spec.resources &&
                                        spec.resources?.length > 0 &&
                                        spec.resources?.map((resource, j) => (
                                            <div
                                                key={j}
                                                style={{
                                                    padding: '5px 20px',
                                                    borderRadius: '4px',
                                                    border: '1px solid #E2E7F0',
                                                    // display: 'flex',
                                                    alignItems: 'center',
                                                }}
                                            >
                                                <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                                                    {t('Resource')}:&nbsp;
                                                </span>
                                                <span> {resource?.type}</span>
                                                <div style={{ marginTop: '3px' }} />
                                                <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                                                    {t('Resource Amount')}:&nbsp;
                                                </span>
                                                <span>{resource?.request}</span>
                                                <br />
                                            </div>
                                        ))}
                                </div>
                            </div>
                        )
                    })}
                <div style={{ display: rawType ? 'block' : 'none' }}>
                    <FormItem label='' required name='stepSpecOverWrites'>
                        <Editor height='500px' width='960px' defaultLanguage='yaml' theme='vs-dark' />
                    </FormItem>
                </div>
            </div>
        </>
    )
}

export default FormFieldModel
