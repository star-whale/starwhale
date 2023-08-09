import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useRuntimeVersion } from '@/domain/runtime/hooks/useRuntimeVersion'
import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { Toggle } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { fetchRuntimeVersion, updateRuntimeVersionShared } from '@/domain/runtime/services/runtimeVersion'
import { useParams } from 'react-router-dom'
import { MonoText } from '@/components/Text'
import { useProject } from '@project/hooks/useProject'
import { getAliasStr } from '@base/utils/alias'

export default function RuntimeVersionOverview() {
    const { projectId, runtimeId, runtimeVersionId } = useParams<{
        projectId: string
        runtimeId: string
        runtimeVersionId: string
    }>()
    const { runtimeVersion, setRuntimeVersion } = useRuntimeVersion()
    const { project } = useProject()

    const [t] = useTranslation()

    const items = [
        {
            label: t('sth name', [t('Runtime')]),
            value: runtimeVersion?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: <MonoText>{runtimeVersion?.versionInfo.name ?? '-'} </MonoText>,
        },
        {
            label: t('Aliases'),
            value: runtimeVersion ? <Alias alias={getAliasStr(runtimeVersion.versionInfo)} /> : null,
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
                    <Shared shared={runtimeVersion?.versionInfo.shared} isTextShow />
                    <Toggle
                        value={runtimeVersion?.versionInfo.shared === 1}
                        onChange={async (v) => {
                            try {
                                await updateRuntimeVersionShared(projectId, runtimeId, runtimeVersionId, v)
                                const data = await fetchRuntimeVersion(projectId, runtimeId, runtimeVersionId)
                                setRuntimeVersion(data)
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
                runtimeVersion?.versionInfo.createdTime &&
                formatTimestampDateTime(runtimeVersion.versionInfo.createdTime),
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
