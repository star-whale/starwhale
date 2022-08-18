import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { useParams } from 'react-router-dom'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import IconFont from '@/components/IconFont'

export default function ProjectSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const project = projectInfo?.data
    const projectName = project?.name || t('PROJECT')

    const navItems: INavItem[] = useMemo(() => {
        if (!project) {
            return []
        }

        return [
            {
                title: t('Models'),
                path: `/projects/${projectId}/models`,
                icon: <IconFont type='model' size={16} />,
                activePathPattern: /\/(models)\/?/,
            },
            {
                title: t('Datasets'),
                path: `/projects/${projectId}/datasets`,
                activePathPattern: /\/(datasets)\/?/,
                icon: <IconFont type='dataset' size={16} />,
            },
            {
                title: t('Runtimes'),
                path: `/projects/${projectId}/runtimes`,
                activePathPattern: /\/(runtimes|new_runtime)\/?/,
                icon: <IconFont type='runtime' size={16} />,
            },
            // {
            //     title: t('Job'),
            //     path: `/projects/${projectId}/jobs`,
            //     activePathPattern: /\/(jobs|new_job)\/?/,
            //     icon: <IconFont type='job' kind='white' size={20} />,
            // },
            {
                title: t('Evaluations'),
                path: `/projects/${projectId}/evaluations`,
                activePathPattern: /\/(evaluations|new_job)\/?/,
                icon: <IconFont type='job' size={16} />,
            },
        ]
    }, [project, projectId, t])
    return (
        <BaseSidebar
            navItems={navItems}
            style={style}
            title={projectName}
            icon={<IconFont type='project' size={20} />}
        />
    )
}
