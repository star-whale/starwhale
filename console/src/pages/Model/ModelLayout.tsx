import { useModel, useModelLoading } from '@model/hooks/useModel'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchModel } from '@model/services/model'
import BaseSubLayout from '@/pages/BaseSubLayout'
import Accordion from '@/components/Accordion'
import { formatTimestampDateTime } from '@/utils/datetime'
import { Panel } from 'baseui/accordion'

export interface IModelLayoutProps {
    children: React.ReactNode
}

export default function ModelLayout({ children }: IModelLayoutProps) {
    const { projectId, modelId } = useParams<{ modelId: string; projectId: string }>()
    const modelInfo = useQuery(`fetchModel:${projectId}:${modelId}`, () => fetchModel(projectId, modelId))
    const { model, setModel } = useModel()
    const { setModelLoading } = useModelLoading()
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

    const items = [
        {
            label: t('Version Name'),
            value: model?.versionName ?? '',
        },
        {
            label: t('Version Meta'),
            value: model?.versionMeta ?? '',
        },
        {
            label: t('Version Tag'),
            value: model?.versionTag ?? '',
        },
        {
            label: t('Model ID'),
            value: model?.id ?? '',
        },
        {
            label: t('Created'),
            value: model?.createdTime && formatTimestampDateTime(model.createdTime),
        },
    ]

    const info = (
        <div
            style={{
                fontSize: '14px',
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))',
                gap: '12px',
            }}
        >
            {items.map((v) => (
                <div key={v?.label} style={{ display: 'flex', gap: '12px' }}>
                    <div
                        style={{
                            lineHeight: '24px',
                            borderRadius: '4px',
                            color: 'rgba(2,16,43,0.60)',
                        }}
                    >
                        {v?.label}:
                    </div>
                    <div> {v?.value}</div>
                </div>
            ))}
        </div>
    )

    const header = React.useMemo(
        () => (
            <div className='mb-20'>
                <Accordion accordion>
                    <Panel title={`${t('Model ID')}: ${model?.id ?? ''}`}>{info}</Panel>
                </Accordion>
            </div>
        ),
        [model, info, t]
    )

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} header={header}>
            {children}
        </BaseSubLayout>
    )
}
