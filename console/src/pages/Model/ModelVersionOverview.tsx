import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useModelVersion } from '../../domain/model/hooks/useModelVersion'
import { MonoText } from '@/components/Text'
import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { Toggle } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { useParams } from 'react-router-dom'
import { fetchModelVersion, updateModelVersionShared } from '@/domain/model/services/modelVersion'

export default function ModelVersionOverview() {
    const { projectId, modelId, modelVersionId } = useParams<{
        projectId: string
        modelId: string
        modelVersionId: string
    }>()
    const { modelVersion, setModelVersion } = useModelVersion()

    const [t] = useTranslation()
    const items = [
        {
            label: t('sth name', [t('Model')]),
            value: modelVersion?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: <MonoText>{modelVersion?.versionName ?? '-'} </MonoText>,
        },
        {
            label: t('Aliases'),
            value: <Alias alias={modelVersion?.versionAlias} />,
        },
        {
            label: t('Shared'),
            value: (
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        height: '100%',
                        gap: '4px',
                    }}
                >
                    <Shared shared={modelVersion?.shared} isTextShow />
                    <Toggle
                        value={modelVersion?.shared === 1}
                        onChange={async (v) => {
                            try {
                                await updateModelVersionShared(projectId, modelId, modelVersionId, v)
                                const data = await fetchModelVersion(projectId, modelId, modelVersionId)
                                setModelVersion(data)
                                toaster.positive(t('dataset.overview.shared.success'))
                            } catch (e) {
                                toaster.negative(t('dataset.overview.shared.fail'))
                            }
                        }}
                    />
                </div>
            ),
        },
        {
            label: t('Created At'),
            value: modelVersion?.createdTime && formatTimestampDateTime(modelVersion.createdTime),
        },
    ]

    return (
        <div className='flex-column'>
            {items.map((v) => (
                <div
                    key={v?.label}
                    style={{
                        display: 'flex',
                        gap: '20px',
                        borderBottom: '1px solid #EEF1F6',
                        lineHeight: '44px',
                        flexWrap: 'nowrap',
                        fontSize: '14px',
                        paddingLeft: '12px',
                    }}
                >
                    <div
                        style={{
                            flexBasis: '110px',
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
}
