import React from 'react'
import { useParams } from 'react-router-dom'
import JobTasks from '../Job/JobTasks'
import { api } from '@/api'

export default function FineTuneRunsTaskListCard() {
    const { projectId, spaceId, fineTuneId } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)
    const { job } = info.data || {}

    if (!job) return null

    return (
        <JobTasks
            params={{
                projectId,
                jobId: job?.id,
            }}
        />
    )
}
