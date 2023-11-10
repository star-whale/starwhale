import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useModelVersion } from '@model/hooks/useModelVersion'
import { MonoText } from '@/components/Text'
import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { Toggle, Text } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { useParams } from 'react-router-dom'
import { fetchModelVersion, updateModelVersionShared } from '@/domain/model/services/modelVersion'
import { useProject } from '@project/hooks/useProject'
import { getAliasStr } from '@base/utils/alias'
import _ from 'lodash'
import { IHasTagSchema } from '@base/schemas/resource'

export default function ModelVersionOverview() {
    const { projectId, modelId, modelVersionId } = useParams<{
        projectId: string
        modelId: string
        modelVersionId: string
    }>()
    const { modelVersion, setModelVersion } = useModelVersion()
    const { project } = useProject()

    const [t] = useTranslation()

    const stepSpecs = modelVersion?.versionInfo.stepSpecs
    const stepSourceGroup = _.groupBy(stepSpecs, 'job_name')

    const items = [
        {
            label: t('sth name', [t('Model')]),
            value: modelVersion?.name ?? '-',
        },
        {
            label: t('Version Name'),
            value: <MonoText maxWidth='400px'>{modelVersion?.versionInfo.name ?? '-'} </MonoText>,
        },
        {
            label: t('Aliases'),
            value: modelVersion ? <Alias alias={getAliasStr(modelVersion.versionInfo as IHasTagSchema)} /> : null,
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
                        value={modelVersion?.versionInfo.shared}
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
        {
            label: t('model.handler'),
            value: (
                <div className='py-10px'>
                    {Object.entries(stepSourceGroup).map(([handler, stepSource]) => {
                        return (
                            <div key={handler}>
                                <div>
                                    <Text content={handler}>{handler}</Text>
                                </div>
                                {stepSource?.map((spec, i) => {
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
                                                    <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                                                        {t('Step')}:&nbsp;
                                                    </span>
                                                    <span>{spec?.name}</span>
                                                    <div style={{ marginTop: '3px' }} />
                                                    <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                                                        {t('Task Amount')}:&nbsp;
                                                    </span>
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
                            </div>
                        )
                    })}
                </div>
            ),
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
