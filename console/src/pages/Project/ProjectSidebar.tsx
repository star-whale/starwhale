import useTranslation from '@/hooks/useTranslation'
import { BsFolder2 } from 'react-icons/bs'
import { BiBarChartSquare, BiLayer, BiEqualizer } from 'react-icons/bi'

import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { useParams } from 'react-router'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export default function ProjectSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ modelId: string; projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const project = projectInfo?.data
    const projectName = project?.name || t('PROJECT')

    console.log(project?.name, projectInfo)
    const navItems: INavItem[] = useMemo(() => {
        if (!project) {
            return []
        }

        return [
            {
                title: t('Model'),
                path: `/projects/${projectId}/models`,
                icon: BiLayer,
                activePathPattern: /\/(models)\/?/,
            },
            {
                title: t('Dataset'),
                path: `/projects/${projectId}/datasets`,
                activePathPattern: /\/(datasets)\/?/,
                icon: BiBarChartSquare,
            },
            {
                title: t('Job'),
                path: `/projects/${projectId}/jobs`,
                activePathPattern: /\/(jobs|new_job)\/?/,
                icon: BiEqualizer,
            },
        ]
    }, [project, projectInfo?.data, projectId, t])
    return <BaseSidebar navItems={navItems} style={style} title={projectName} icon={BiBarChartSquare} />
}
