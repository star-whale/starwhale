import { useProject, useProjectLoading } from '@project/hooks/useProject'
import useTranslation from '@/hooks/useTranslation'
import { fetchProject } from '@project/services/project'
import { RiSurveyLine } from 'react-icons/ri'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { resourceIconMapping } from '@/consts'
import { FiActivity } from 'react-icons/fi'
import { useParams } from 'react-router'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export default function ProjectSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    // const { project, setProject } = useProject()
    const { projectId, modelId } = useParams<{ modelId: string; projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const project = projectInfo.data

    const navItems: INavItem[] = useMemo(
        () =>
            project
                ? [
                      {
                          title: project?.name ?? t('overview'),
                          path: `/projects/${project?.id}`,
                          icon: RiSurveyLine,
                      },
                      {
                          title: t('models'),
                          path: `/projects/${project.id}/models`,
                          // icon: resourceIconMapping.model,
                          activePathPattern: /^\/(models)\/?/,
                      },
                      {
                          title: t('datasets'),
                          path: `/projects/${project.id}/datasets`,
                          activePathPattern: /^\/(datasets)\/?/,
                      },
                      {
                          title: t('jobs'),
                          path: `/projects/${project.id}/jobs`,
                          activePathPattern: /^\/(jobs|new_job)\/?/,
                      },
                  ]
                : [],
        [project, t]
    )
    return <BaseSidebar navItems={navItems} style={style} />
}
