import { useModel, useModelLoading } from '@model/hooks/useModel'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo, useState } from 'react'
import { useQuery } from 'react-query'
import { useHistory, useLocation, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchModel, removeModel } from '@model/services/model'
import BaseSubLayout from '@/pages/BaseSubLayout'
import IconFont from '@starwhale/ui/IconFont'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import { Button } from '@starwhale/ui'
import { usePage } from '@/hooks/usePage'
import qs from 'qs'
import Checkbox from '@/components/Checkbox'
import { STYLE_TYPE } from 'baseui/checkbox'
import { createUseStyles } from 'react-jss'
import { useQueryArgs } from '@starwhale/core'
import { ConfirmButton } from '@/components/Modal/confirm'
import { toaster } from 'baseui/toast'
import { useFetchModelVersion } from '../../domain/model/hooks/useFetchModelVersion'
import { useModelVersion } from '../../domain/model/hooks/useModelVersion'
import ModelVersionSelector from '../../domain/model/components/ModelVersionSelector'

const useStyles = createUseStyles({
    tagWrapper: {
        position: 'relative',
    },
    tag: {
        position: 'absolute',
        background: '#EBF1FF',
        borderRadius: '12px',
        padding: '3px 10px',
        top: '5px',
        right: '30px',
    },
})

export interface IModelLayoutProps {
    children: React.ReactNode
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

    const modelVersionInfo = useFetchModelVersion(projectId, modelId, modelVersionId)

    useEffect(() => {
        setModelLoading(modelInfo.isLoading)
        if (modelInfo.isSuccess) {
            if (modelInfo.data.versionMeta !== model?.versionMeta) {
                setModel(modelInfo.data)
            }
        } else if (modelInfo.isLoading) {
            setModel(undefined)
        }
    }, [model?.versionMeta, modelInfo.data, modelInfo.isLoading, modelInfo.isSuccess, setModel, setModelLoading])

    useEffect(() => {
        if (modelVersionInfo.data) {
            setModelVersion(modelVersionInfo.data)
        }
    }, [modelVersionInfo.data, setModelVersion])

    const [t] = useTranslation()
    const modelName = model?.versionMeta ?? '-'
    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Models'),
                path: `/projects/${projectId}/models`,
            },
            {
                title: modelName,
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
                title: t('Files'),
                path: `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/files`,
                pattern: '/\\/files\\/?',
            },
        ]
        return items
    }, [projectId, modelId, modelVersionId, t])

    const location = useLocation()
    const activeItemId = useMemo(() => {
        const item = navItems
            .slice()
            .reverse()
            .find((item_) => (location.pathname ?? '').startsWith(item_.path ?? ''))
        const paths = item?.path?.split('/') ?? []
        return paths[paths.length - 1] ?? 'files'
    }, [location.pathname, navItems])

    const extra = useMemo(() => {
        return (
            <ConfirmButton
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
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
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
                                                width: isCompare ? '50%' : '100%',
                                            },
                                        },
                                    }}
                                />
                                {isCompare && <div className={styles.tag}>{t('model.viewer.source')}</div>}
                            </div>

                            <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
                                <Checkbox
                                    checked={isCompare}
                                    onChange={(e) => {
                                        setIsCompare(e.currentTarget.checked)
                                        if (!e.currentTarget.checked)
                                            history.push(
                                                `/projects/${projectId}/models/${modelId}/versions/${modelVersionId}/${activeItemId}?${qs.stringify(
                                                    { ...page, compare: undefined }
                                                )}`
                                            )
                                    }}
                                    checkmarkType={STYLE_TYPE.toggle_round}
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
                                                    width: '50%',
                                                },
                                            },
                                        }}
                                    />
                                    <div className={styles.tag}>{t('model.viewer.target')}</div>
                                </div>
                            )}
                        </div>
                        <Button
                            overrides={{
                                Root: {
                                    style: {
                                        justifySelf: 'flex-end',
                                    },
                                },
                            }}
                            as='withIcon'
                            startEnhancer={() => <IconFont type='runtime' />}
                            onClick={() => history.push(`/projects/${projectId}/models/${modelVersionId}`)}
                        >
                            {t('History')}
                        </Button>
                    </div>
                )}
                {modelVersionId && <BaseNavTabs navItems={navItems} />}
                <div style={{ flex: '1', display: 'flex', flexDirection: 'column' }}>{children}</div>
            </div>
        </BaseSubLayout>
    )
}
