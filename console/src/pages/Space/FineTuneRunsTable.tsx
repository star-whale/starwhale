import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { ExtendButton, VersionText, MonoText } from '@starwhale/ui'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { IFineTuneVo, IPageInfoFineTuneVo, api } from '@/api'
import { useHistory, useParams } from 'react-router-dom'
import { usePage } from '@/hooks/usePage'
import User from '@/domain/user/components/User'
import Table from '@/components/Table'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'

export default function FineTuneRunsTable({ data, isLoading }: { data?: IPageInfoFineTuneVo; isLoading?: boolean }) {
    const [t] = useTranslation()
    const history = useHistory()
    const { projectId } = useParams<{ projectId: any }>()

    const getActions = ({ job }: IFineTuneVo) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/jobs/${job.id}/overview`)}
                >
                    {hasText ? t('View Details') : undefined}
                </ExtendButton>
            ),
        },
    ]

    const { columns } = useFineTuneColumns()

    return (
        <Table
            renderActions={(rowIndex) => {
                const ft = data?.list?.[rowIndex]
                if (!ft) return undefined
                return getActions(ft)
            }}
            isLoading={isLoading}
            columns={columns.map((v) => v.title)}
            data={data?.list?.map((ft) => {
                return columns.map((v) => <v.renderCell key={v.key} value={v.mapDataToValue?.(ft)} />)
            })}
        />
    )
}
