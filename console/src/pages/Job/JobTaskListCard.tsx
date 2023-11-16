import React from 'react'
import { useParams } from 'react-router-dom'
import JobTasks from './JobTasks'

export default function JobTaskListCard() {
    const params = useParams<{ projectId: string; jobId: string }>()
    return <JobTasks params={params} />
}
