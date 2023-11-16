import { useModel, useModelLoading } from '@model/hooks/useModel'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo, useState } from 'react'
import { useQuery } from 'react-query'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchModel, removeModel } from '@model/services/model'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import { Button, Toggle } from '@starwhale/ui'
import { usePage } from '@/hooks/usePage'
import qs from 'qs'
import { createUseStyles } from 'react-jss'
import { useQueryArgs } from '@starwhale/core'
import { ConfirmButton } from '@starwhale/ui/Modal'
import { toaster } from 'baseui/toast'
import { useFetchModelVersion } from '@model/hooks/useFetchModelVersion'
import { useModelVersion } from '@model/hooks/useModelVersion'
import ModelVersionSelector from '@model/components/ModelVersionSelector'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'
import { listJobs } from '@job/services/job'
import { JobStatusType } from '@job/schemas/job'
import { IJobVo } from '@/api'

const useStyles = createUseStyles({
    tagWrapper: {
        position: 'relative',
    },
    tag: {
        position: 'absolute',
        background: '#EBF1FF',
        borderRadius: '12px',
        padding: '3px 10px',
        top: 'calc(50% - 9px)',
        right: '28px',
        fontSize: '12px',
        lineHeight: '12px',
    },
})

export interface IModelLayoutProps {
    children: React.ReactNode
}

interface IModelVersionContextProps {
    modelVersionId: string
    servingJobs?: IJobVo[]
}

const ModelVersionContext = React.createContext<IModelVersionContextProps | undefined>(undefined)
export const useModelVersionContext = () => {
    const ctx = React.useContext(ModelVersionContext)
    if (!ctx) {
        throw new Error('useModelVersionContext must be used within a ModelVersionContextProvider')
    }
    return ctx
}

export default function ModelOverviewLayout({ children }: IModelLayoutProps) {
    const styles = useStyles()
    const { projectId, modelId, modelVersionId } = useParams<{
        modelId: string
        projectId: string
        modelVersionId: string
    }>()
    const modelInfo = useQuery(`fetchModel:${projectId}:${modelId}`, () => fetchModel(projectId, modelId))
    const { model, setModel } = useModel()
    const { setModelLoading } = useModelLoading()
    const [page] = usePage()
    const history = useHistory()
    const { setModelVersion } = useModelVersion()
    const { query } = useQueryArgs()
    const [servingJobs, setServingJobs] = useState<IJobVo[]>()

    const modelVersionInfo = useFetchModelVersion(projectId, modelId, modelVersionId)

    useEffect(() => {
        setModelLoading(modelInfo.isLoading)
        if (modelInfo.isSuccess) {
            if (modelInfo.data.versionInfo.name !== model?.versionName) {
                setModel(modelInfo.data)
            }
        } else if (modelInfo.isLoading) {
            setModel(undefined)
        }
    }, [model?.versionName, modelInfo.data, modelInfo.isLoading, modelInfo.isSuccess, setModel, setModelLoading])

    useEffect(() => {
        if (modelVersionInfo.data) {
            setModelVersion(modelVersionInfo.data)
        }
    }, [modelVersionInfo.data, setModelVersion])

    useEffect(() => {
        listJobs(projectId, { swmpId: modelVersionId, pageNum: 1, pageSize: 9999 }).then((res) => {
            const jobWithExposed = res?.list?.filter((job) => {
                return job.jobStatus === JobStatusType.RUNNING && job.exposedLinks && job.exposedLinks.length > 0
            })
            setServingJobs(jobWithExposed)
        })
    }, [modelVersionId, projectId])

    const [t] = useTranslation()
    const modelName = model?.name ?? '-'
    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Models'),
                path: `/projects/${projectId}/models`,
            },
            {
                title: modelName || '-',
                path: `/projects/${projectId}/models/${modelId}`,
            },
        ]
        return items
    }, [projectId, modelName, modelId, t])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/overview`,
                pattern: '/\\/overview\\/?',
            },
            {
                title: 'README',
                path: `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/readme`,
                pattern: '/\\/overview\\/?',
            },
            {
                title: t('Files'),
                path: `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/files`,
                pattern: '/\\/files\\/?',
            },
        ]

        if (servingJobs?.length) {
            items.push({
                title: t('Servings'),
                path: `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/servings`,
                pattern: '/\\/servings\\/?',
            })
        }
        return items
    }, [t, projectId, modelId, modelVersionId, servingJobs?.length])

    const { activeItemId } = useRouterActivePath(navItems)

    const extra = useMemo(() => {
        return (
            <ConfirmButton
                as='negative'
                title={t('model.remove.confirm')}
                onClick={async () => {
                    await removeModel(projectId, modelId)
                    toaster.positive(t('model.remove.success'), { autoHideDuration: 1000 })
                    history.push(`/projects/${projectId}/models`)
                }}
            >
                {t('model.remove.button')}
            </ConfirmButton>
        )
    }, [projectId, modelId, history, t])

    const [isCompare, setIsCompare] = useState(!!query.compare ?? false)

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} extra={extra}>
            <div className='content-full'>
                {modelVersionId && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: '280px 120px 280px', gap: '21px' }}>
                            <div className={styles.tagWrapper}>
                                <ModelVersionSelector
                                    projectId={projectId}
                                    modelId={modelId}
                                    value={modelVersionId}
                                    onChange={(v) =>
                                        history.push(
                                            `/projects/${projectId}/models/${modelId}/versions/${v}/${activeItemId}?${qs.stringify(
                                                page
                                            )}`
                                        )
                                    }
                                    overrides={{
                                        SingleValue: {
                                            style: {
                                                width: isCompare ? '70%' : '100%',
                                            },
                                        },
                                    }}
                                />
                                {isCompare && <div className={styles.tag}>{t('model.viewer.source')}</div>}
                            </div>

                            <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
                                <Toggle
                                    value={isCompare}
                                    onChange={(checked) => {
                                        setIsCompare(checked)
                                        if (!checked)
                                            history.push(
                                                `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/${activeItemId}?${qs.stringify(
                                                    { ...page, compare: undefined }
                                                )}`
                                            )
                                    }}
                                />
                                {t('model.viewer.compare')}
                            </div>

                            {isCompare && (
                                <div className={styles.tagWrapper}>
                                    <ModelVersionSelector
                                        projectId={projectId}
                                        modelId={modelId}
                                        value={query.compare}
                                        onChange={(v) =>
                                            history.push(
                                                `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/${activeItemId}?${qs.stringify(
                                                    { ...page, compare: v }
                                                )}`
                                            )
                                        }
                                        overrides={{
                                            SingleValue: {
                                                style: {
                                                    width: '70%',
                                                },
                                            },
                                        }}
                                    />
                                    <div className={styles.tag}>{t('model.viewer.target')}</div>
                                </div>
                            )}
                        </div>
                        <Button
                            kind='secondary'
                            icon='time'
                            onClick={() => history.push(`/projects/${projectId}/models/${modelId}`)}
                        >
                            {t('History')}
                        </Button>
                    </div>
                )}
                {modelVersionId && <BaseNavTabs navItems={navItems} />}
                <div className='content-full-scroll'>
                    <ModelVersionContext.Provider value={{ modelVersionId, servingJobs }}>
                        {children}
                    </ModelVersionContext.Provider>
                </div>
            </div>
        </BaseSubLayout>
    )
}
