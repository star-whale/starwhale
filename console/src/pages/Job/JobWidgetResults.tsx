import React, { useCallback, useEffect } from 'react'
import { FullTablesEditor } from '@/components/Editor/FullTablesEditor'
import { useParams } from 'react-router-dom'
import { Select } from '@starwhale/ui'
import { useJob } from '@/domain/job/hooks/useJob'
import { fetchPanelSetting, updatePanelSetting } from '@/domain/panel/services/panel'
import { toaster } from 'baseui/toast'
import { fetchModelVersionPanelSetting } from '@model/services/modelVersion'
import { getToken } from '@/api'
import { tryParseSimplified } from '@/domain/panel/utils'
import { useProject } from '@project/hooks/useProject'
import useTranslation from '@/hooks/useTranslation'

interface Layout {
    name: string
    content: string | object
    label: string
}

function JobWidgetResults() {
    const [t] = useTranslation()
    const { projectId: projectFromUri } = useParams<{ jobId: string; projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id ?? projectFromUri
    const { job } = useJob()
    const storeKey = job?.modelName ? ['evaluation-model', job?.modelName, job?.modelVersion].join('-') : ''
    const [currentLayout, setCurrentLayout] = React.useState<Layout | undefined>(undefined)
    const [layouts, setLayouts] = React.useState<Layout[]>([])
    const onStateChange = async (data: any) => {
        await updatePanelSetting(projectId, storeKey, data)
        toaster.positive(t('panel.save.success'), { autoHideDuration: 2000 })
    }

    const updateLayout = useCallback((layout: Layout) => {
        setLayouts((prevState) => {
            const others = prevState.filter((i) => i.name !== layout.name)
            others.push(layout)
            return others
        })
    }, [])

    useEffect(() => {
        fetchModelVersionPanelSetting(project?.name, job?.modelName, job?.modelVersion, getToken()).then((data) => {
            if (!data) return
            const layout = tryParseSimplified(data) ?? data
            updateLayout({ name: 'model-builtin', content: layout, label: t('panel.view.config.model-buildin') })
        })
    }, [project, job, updateLayout, t])

    useEffect(() => {
        if (!storeKey) {
            return
        }
        fetchPanelSetting(projectId, storeKey).then((data) => {
            if (!data) return
            // try simplified version for standalone usage
            const parsed = tryParseSimplified(JSON.parse(data)) ?? data
            const layout = { name: 'custom', content: parsed, label: t('panel.view.config.custom') }
            setCurrentLayout(layout)
            updateLayout(layout)
        })
    }, [projectId, job, storeKey, updateLayout, t])

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            <div style={{ height: '50px', display: 'flex', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex' }}>
                    <Select
                        overrides={{
                            ControlContainer: {
                                style: {
                                    width: '200px',
                                },
                            },
                        }}
                        clearable={false}
                        options={layouts.map((layout) => ({ id: layout.name, label: layout.label }))}
                        value={currentLayout ? [{ id: currentLayout.name, label: currentLayout.name }] : []}
                        onChange={({ value }) => {
                            const layout = layouts.find((l) => l.name === value[0].id)
                            if (layout) setCurrentLayout(layout)
                        }}
                    />
                </div>
            </div>
            <FullTablesEditor initialState={currentLayout?.content} onStateChange={onStateChange} />
        </div>
    )
}
export default JobWidgetResults
