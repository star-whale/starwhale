import useTranslation from '@/hooks/useTranslation'
import { BsFolder2 } from 'react-icons/bs'
import { BiBarChartSquare, BiLayer, BiEqualizer } from 'react-icons/bi'

import React, { useEffect, useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { useParams } from 'react-router'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export default function ProjectSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const { projectId, modelId } = useParams<{ modelId: string; projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const project = projectInfo.data

    const navItems: INavItem[] = useMemo(
        () =>
            project
                ? [
                      {
                          title: project?.name ?? t('sth name', [t('Project')]),
                          path: `/projects/${project?.id}`,
                          icon: BsFolder2,
                      },
                      {
                          title: t('Model'),
                          path: `/projects/${project.id}/models`,
                          icon: BiLayer,
                          activePathPattern: /\/(models)\/?/,
                      },
                      {
                          title: t('Dataset'),
                          path: `/projects/${project.id}/datasets`,
                          activePathPattern: /\/(datasets)\/?/,
                          icon: BiBarChartSquare,
                      },
                      {
                          title: t('Job'),
                          path: `/projects/${project.id}/jobs`,
                          activePathPattern: /\/(jobs|new_job)\/?/,
                          icon: BiEqualizer,
                      },
                  ]
                : [],
        [project, t]
    )
    return <BaseSidebar navItems={navItems} style={style} />
}
