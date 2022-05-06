import { useProject, useProjectLoading } from '@project/hooks/useProject'
import React, { useEffect } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { fetchProject } from '@project/services/project'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface IProjectLayoutProps {
    children: React.ReactNode
}

export default function ProjectLayout({ children }: IProjectLayoutProps) {
    const { projectId } = useParams<{ projectId: string }>()
    const projectInfo = useQuery(`fetchProject:${projectId}`, () => fetchProject(projectId))
    const { project, setProject } = useProject()
    const { setProjectLoading } = useProjectLoading()
    useEffect(() => {
        setProjectLoading(projectInfo.isLoading)
        if (projectInfo.isSuccess) {
            if (projectInfo.data.id !== project?.id) {
                setProject(projectInfo.data)
            }
        } else if (projectInfo.isLoading) {
            setProject(undefined)
        }
    }, [project?.id, projectInfo.data, projectInfo.isLoading, projectInfo.isSuccess, setProject, setProjectLoading])

    return <BaseSubLayout>{children}</BaseSubLayout>
}
