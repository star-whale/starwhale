import React from 'react'
import { useParams } from 'react-router-dom'

export default function ReportEdit() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { projectId, reportId } = useParams<{
        projectId: string
        reportId: string
    }>()

    return <></>
}
