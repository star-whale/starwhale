import { useProject, useProjectLoading } from '@project/hooks/useProject'
import React, { useEffect } from 'react'
import { useQuery } from 'react-query'
import { Redirect, useParams } from 'react-router-dom'
import { fetchProject } from '@project/services/project'
import BaseSubLayout from '@/pages/BaseSubLayout'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { toaster } from 'baseui/toast'
import QuickStart from '@/domain/project/components/QuickStart'

export interface IProjectLayoutProps {
    children: React.ReactNode
}

function ProjectLayout({ children }: IProjectLayoutProps) {
    const { projectId } = useParams<{ projectId: string }>()
    const projectInfo = useQuery(`fetchProject:${projectId}`, () => fetchProject(projectId))
    const { project, setProject } = useProject()
    const { setProjectLoading } = useProjectLoading()
    useEffect(() => {
        setProjectLoading(projectInfo.isLoading)
        if (projectInfo.isSuccess) {
            if (projectInfo.data?.id !== project?.id) {
                setProject(projectInfo.data)
            }
        } else if (projectInfo.isLoading) {
            setProject(undefined)
        }
    }, [project?.id, projectInfo.data, projectInfo.isLoading, projectInfo.isSuccess, setProject, setProjectLoading])

    if (projectInfo.isLoading) {
        return <BusyPlaceholder />
    }

    if (projectInfo.isSuccess && !projectInfo.data) {
        toaster.negative('project no access', { autoHideDuration: 2000 })
        return <Redirect to='/' />
    }

    return <BaseSubLayout>{children}</BaseSubLayout>
}

ProjectLayout.displayName = 'ProjectLayout'
export default ProjectLayout
