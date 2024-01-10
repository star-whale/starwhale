import React from 'react'
import { FormSelect, Toggle } from '@starwhale/ui/Select'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { useParams } from 'react-router-dom'
import { ICreateJobFormSchema } from '../schemas/job'
import ModelTreeSelector from '@/domain/model/components/ModelTreeSelector'
import { EventEmitter } from 'ahooks/lib/useEventEmitter'
import MonacoEditor from '@starwhale/ui/MonacoEditor'
import { createUseStyles } from 'react-jss'
import yaml from 'js-yaml'
import { toaster } from 'baseui/toast'
import { IStepSpec } from '@/api'
import { WidgetForm } from '@starwhale/core/form'
import { convertToRJSF } from '../utils'
import { Button } from '@starwhale/ui'
import { getReadableStorageQuantityStr } from '@/utils'
import { useSelections, useSetState } from 'ahooks'

const useStyles = createUseStyles({
    modelField: {
        display: 'grid',
        columnGap: 40,
        gridTemplateColumns: '660px 280px 180px',
        gridTemplateRows: 'minmax(0px, max-content)',
    },
    rjsfForm: {
        '& .control-label': {
            flexBasis: '170px !important',
            width: '170px !important',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
        },
    },
})

function FormFieldModel({
    form,
    FormItem,
    eventEmitter,
    stepSource,
    setModelTree,
    fullStepSource,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    eventEmitter: EventEmitter<any>
    stepSource?: IStepSpec[]
    setModelTree: (obj: any) => void
    fullStepSource?: IStepSpec[]
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
                    fullStepSource.filter((v: IStepSpec) => v?.job_name === _changes.modelVersionHandler)
                ),
            })
        }
    })

    const _modelVersionUrl = form.getFieldValue('modelVersionUrl')
    const rawType = form.getFieldValue('rawType')
    const modelVersionHandler = form.getFieldValue('modelVersionHandler')

    const [RJSFData, setRJSFData] = useSetState<any>({})
    const getRJSFFormSchema = React.useCallback((currentStepSource) => {
        const extrUISchema = {
            'ui:submitButtonOptions': { norender: true },
        }
        const { schema, uiSchema } = convertToRJSF(currentStepSource ?? [])
        return {
            schemas: {
                schema,
                uiSchema: {
                    ...uiSchema,
                    ...extrUISchema,
                },
            },
        }
    }, [])

    const StepLabel = ({ label, value }) => (
        <>
            <span style={{ color: 'rgba(2,16,43,0.60)' }}>{label}:&nbsp;</span>
            <span>{value}</span>
        </>
    )

    const SourceLabel = ({ label, value }) => {
        let _v = value
        if (label === 'memory') {
            _v = getReadableStorageQuantityStr(value)
        }
        return (
            <div className='flex flex-col items-center lh-normal'>
                <span style={{ color: 'rgba(2,16,43,0.60)' }}>{label}</span>
                <span>{_v}</span>
            </div>
        )
    }

    const { isSelected, toggle } = useSelections<any>([])

    // if RJSFData changed, then update stepSpecOverWrites
    // splice RJSFData by key.split('-') find the right stepSpec and update it
    // then update stepSpecOverWrites
    React.useEffect(() => {
        if (!stepSource || Object.keys(RJSFData).length === 0) return
        const newStepSource = JSON.parse(JSON.stringify(stepSource))
        Object.entries(RJSFData).forEach(([key, value]) => {
            const [jobName, argument, field] = key.split('@@@')
            newStepSource?.forEach((v) => {
                if (v?.arguments?.[argument] && v?.job_name === jobName) {
                    // eslint-disable-next-line
                    v.arguments[argument][field].value = value
                }
            })
        })
        form.setFieldsValue({
            stepSpecOverWrites: yaml.dump(newStepSource),
        })
    }, [form, stepSource, RJSFData])

    // watch stepSource and update RJSFData
    React.useEffect(() => {
        if (!stepSource) return
        const _RJSFData = {}
        stepSource?.forEach((spec) => {
            if (spec?.arguments) {
                Object.entries(spec?.arguments).forEach(([argument, fields]) => {
                    Object.entries(fields as any).forEach(([field, v]) => {
                        const { type, value } = v as any
                        if (type?.param_type === 'BOOL' && typeof value === 'string') {
                            _RJSFData[[spec?.job_name, argument, field].join('@@@')] = value === 'true'
                            return
                        }
                        // eslint-disable-next-line
                        _RJSFData[[spec?.job_name, argument, field].join('@@@')] = value
                    })
                })
            }
        })
        setRJSFData(_RJSFData)
    }, [stepSource, setRJSFData])

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
            <div className='flex pb-0 gap-40px'>
                <div>
                    {stepSource &&
                        stepSource?.length > 0 &&
                        !rawType &&
                        stepSource?.map((spec, i) => {
                            return (
                                <div key={[spec?.name, i].join('')}>
                                    <div className='flex lh-none items-stretch gap-[20px] mb-[10px]'>
                                        <div className='min-w-[660px] rounded-[4px] b-[#E2E7F0] border-1'>
                                            <div className='lh-[30px] bg-[#EEF1F6] px-[20px] py-[5px]'>
                                                <StepLabel label={t('Step')} value={spec?.name} />
                                            </div>
                                            <div className='flex px-[20px] py-[15px] gap-[20px] items-center'>
                                                <SourceLabel label={t('Task Amount')} value={spec?.replicas} />
                                                {spec.resources &&
                                                    spec.resources?.length > 0 &&
                                                    spec.resources?.map((resource, j) => (
                                                        <SourceLabel
                                                            key={j}
                                                            label={resource?.type}
                                                            value={resource?.request}
                                                        />
                                                    ))}
                                                {spec?.arguments && (
                                                    <div className='ml-auto'>
                                                        <Button
                                                            type='button'
                                                            icon={isSelected(spec?.name) ? 'arrow_top' : 'arrow_down'}
                                                            as='link'
                                                            onClick={() => toggle(spec?.name)}
                                                        >
                                                            {t('Parameters')}
                                                        </Button>
                                                    </div>
                                                )}
                                            </div>
                                            {isSelected(spec?.name) && (
                                                <div className={`px-[20px] pb-[20px] gap-[20px] ${styles.rjsfForm}`}>
                                                    <div className='pt-[15px] pb-[25px] color-[rgba(2,16,43,0.60)] b-[#E2E7F0] border-t-1'>
                                                        {t('Parameters')}
                                                    </div>
                                                    <WidgetForm
                                                        form={getRJSFFormSchema([spec])}
                                                        formData={RJSFData}
                                                        onChange={setRJSFData}
                                                    />
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )
                        })}

                    <div style={{ display: rawType ? 'block' : 'none' }}>
                        <FormItem label='' required name='stepSpecOverWrites'>
                            <MonacoEditor height='500px' width='960px' defaultLanguage='yaml' theme='vs-dark' />
                        </FormItem>
                    </div>
                </div>
            </div>
        </>
    )
}

export default FormFieldModel
