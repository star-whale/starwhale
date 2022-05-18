import useTranslation from '@/hooks/useTranslation'
import { BiBarChartSquare, BiLayer, BiEqualizer } from 'react-icons/bi'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { useParams } from 'react-router'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import projectSvg from '@/assets/fonts/project.svg'
import IconFont from '@/components/IconFont'

export default function ProjectSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ modelId: string; projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const project = projectInfo?.data
    const projectName = project?.name || t('PROJECT')

    const navItems: INavItem[] = useMemo(() => {
        if (!project) {
            return []
        }

        return [
            {
                title: t('Model'),
                path: `/projects/${projectId}/models`,
                icon: <IconFont type='model' kind='white' size={20} />,
                activePathPattern: /\/(models)\/?/,
            },
            {
                title: t('Dataset'),
                path: `/projects/${projectId}/datasets`,
                activePathPattern: /\/(datasets)\/?/,
                icon: <IconFont type='dataset' kind='white' size={20} />,
            },
            {
                title: t('Job'),
                path: `/projects/${projectId}/jobs`,
                activePathPattern: /\/(jobs|new_job)\/?/,
                icon: <IconFont type='job' kind='white' size={20} />,
            },
        ]
    }, [project, projectInfo?.data, projectId, t])
    return <BaseSidebar navItems={navItems} style={style} title={projectName} icon={<IconFont type='project' />} />
}
