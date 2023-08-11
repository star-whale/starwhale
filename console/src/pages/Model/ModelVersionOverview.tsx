import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useModelVersion } from '@model/hooks/useModelVersion'
import { MonoText } from '@/components/Text'
import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { Toggle } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { useParams } from 'react-router-dom'
import { fetchModelVersion, updateModelVersionShared } from '@/domain/model/services/modelVersion'
import { useProject } from '@project/hooks/useProject'
import { getAliasStr } from '@base/utils/alias'

export default function ModelVersionOverview() {
    const { projectId, modelId, modelVersionId } = useParams<{
        projectId: string
        modelId: string
        modelVersionId: string
    }>()
    const { modelVersion, setModelVersion } = useModelVersion()
    const { project } = useProject()

    const [t] = useTranslation()
    const items = [
        {
            label: t('sth name', [t('Model')]),
            value: modelVersion?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: <MonoText>{modelVersion?.versionInfo.name ?? '-'} </MonoText>,
        },
        {
            label: t('Aliases'),
            value: modelVersion ? <Alias alias={getAliasStr(modelVersion.versionInfo)} /> : null,
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
                    <Shared shared={modelVersion?.versionInfo.shared} isTextShow />
                    <Toggle
                        value={modelVersion?.versionInfo.shared === 1}
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
            value:
                modelVersion?.versionInfo.createdTime && formatTimestampDateTime(modelVersion?.versionInfo.createdTime),
        },
    ].filter((item) => {
        return project?.privacy === 'PUBLIC' || item.label !== t('Shared')
    })

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
