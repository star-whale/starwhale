import React, { useCallback, useEffect } from 'react'
import { FullTablesEditor } from '@/components/Editor/FullTablesEditor'
import { useParams } from 'react-router-dom'
import { BusyPlaceholder, Button, Select } from '@starwhale/ui'
import { useJob } from '@/domain/job/hooks/useJob'
import { fetchPanelSetting, updatePanelSetting } from '@/domain/panel/services/panel'
import { toaster } from 'baseui/toast'
import { fetchModelVersionPanelSetting } from '@model/services/modelVersion'
import { getToken } from '@/api'
import { tryParseSimplified } from '@/domain/panel/utils'
import { useProject } from '@project/hooks/useProject'
import useTranslation from '@/hooks/useTranslation'
import { PanelChartSaveEvent, SectionAppendEvent } from '@starwhale/core'
import { WithCurrentAuth } from '@/api/WithAuth'

interface Layout {
    name: string
    content: string | object
    label: string
}

function EvaluationWidgetResults() {
    const [t] = useTranslation()
    const { projectId: projectFromUri } = useParams<{ jobId: string; projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id ?? projectFromUri
    const { job } = useJob()
    const storeKey = job?.modelName ? ['ft-evaluation-model', job?.modelName, job?.uuid].join('-') : ''
    const [currentLayout, setCurrentLayout] = React.useState<Layout | undefined>(undefined)
    const [layouts, setLayouts] = React.useState<Layout[]>([])
    const onStateChange = async (data: any) => {
        await updatePanelSetting(projectId, storeKey, data)
        toaster.positive(t('panel.save.success'), { autoHideDuration: 2000 })
    }
    const [isLoading, setIsLoading] = React.useState(true)

    const updateLayout = useCallback((layout: Layout) => {
        setLayouts((prevState) => {
            const others = prevState.filter((i) => i.name !== layout.name)
            others.push(layout)
            return others
        })
    }, [])

    useEffect(() => {
        setIsLoading(true)
        Promise.allSettled<{ value: any }>([
            fetchModelVersionPanelSetting(project?.name, job?.modelName, job?.modelVersion, getToken()),
            fetchPanelSetting(projectId, storeKey),
        ])
            // @ts-ignore
            .then(([{ value: builtin }, { value: custom }]) => {
                if (builtin) {
                    const layout = tryParseSimplified(builtin) ?? builtin
                    updateLayout({
                        name: 'model-builtin',
                        content: layout,
                        label: t('panel.view.config.model-buildin'),
                    })
                }

                if (custom) {
                    const parsed = tryParseSimplified(JSON.parse(custom)) ?? custom
                    const layout2 = { name: 'custom', content: parsed, label: t('panel.view.config.custom') }
                    setCurrentLayout(layout2)
                    updateLayout(layout2)
                }
            })
            .finally(() => {
                setIsLoading(false)
            })
    }, [projectId, project, job, updateLayout, storeKey, t])

    const eventBus = React.useRef<any>()
    const onInit = React.useCallback((args) => {
        eventBus.current = args.eventBus
    }, [])

    if (isLoading) {
        return (
            <BusyPlaceholder
                type='loading'
                style={{
                    overflow: 'hidden',
                }}
            />
        )
    }

    return (
        <div className='flex flex-col content-full'>
            <div className='flex justify-between mb-14px min-w-0'>
                <div className='w-200px'>
                    <Select
                        clearable={false}
                        options={layouts.map((layout) => ({ id: layout.name, label: layout.label }))}
                        value={currentLayout ? [{ id: currentLayout.name, label: currentLayout.name }] : []}
                        onChange={({ value }) => {
                            const layout = layouts.find((l) => l.name === value[0].id)
                            if (layout) setCurrentLayout(layout)
                        }}
                    />
                </div>
                <div className='flex items-center flex-shrink-0 gap-10px'>
                    <WithCurrentAuth id='evaluation.panel.save'>
                        <Button onClick={() => eventBus.current?.publish(new PanelChartSaveEvent())}>
                            {t('panel.save')}
                        </Button>
                    </WithCurrentAuth>
                    <Button onClick={() => eventBus.current?.publish(new SectionAppendEvent())}>
                        {t('panel.add')}
                    </Button>
                </div>
            </div>
            <FullTablesEditor onInit={onInit} initialState={currentLayout?.content} onSave={onStateChange} />
        </div>
    )
}
export default EvaluationWidgetResults
