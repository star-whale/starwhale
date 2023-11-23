import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import JobForm from '@job/components/JobForm'
import { useHistory, useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core/utils'
import { IJobRequest, api } from '@/api'
import { ExtendButton } from '@starwhale/ui'
import { useRouteInlineContext } from '@/contexts/RouteInlineContext'
import FormFieldZoneModel from '@/domain/job/components/FormFieldZoneModel'

export default function FineTuneNewCard() {
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const history = useHistory()
    const { fineTuneId } = query
    const { isInline } = useRouteInlineContext()

    const handleSubmit = useCallback(
        async (data: IJobRequest) => {
            if (!projectId) {
                return
            }
            await api.createJob(projectId, {
                ...data,
                type: 'FINE_TUNE',
                bizType: 'FINE_TUNE',
                bizId: spaceId,
            })
            if (isInline) {
                history.go(-1)
                return
            }
            history.push(`/projects/${projectId}/spaces/${spaceId}/fine-tunes`)
        },
        [projectId, spaceId, isInline, history]
    )
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)
    const job = info?.data?.job

    return (
        <Card
            title={
                <div className='flex gap-10px font-18px font-800'>
                    {!isInline && (
                        <ExtendButton
                            icon='arrow_left'
                            styleas={['iconnormal', 'nopadding']}
                            onClick={() => history.go(-1)}
                        />
                    )}
                    {t('ft.job.new')}
                </div>
            }
        >
            <JobForm
                onSubmit={handleSubmit}
                job={job}
                autoFill={!job}
                enableTemplate={false}
                plugins={{ FormFieldModel: FormFieldZoneModel }}
                validationDatasets={info?.data?.validationDatasets}
            />
        </Card>
    )
}
