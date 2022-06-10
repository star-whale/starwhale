import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { useParams } from 'react-router'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import IconFont from '@/components/IconFont'
import { GrDocker } from 'react-icons/gr'

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
                title: t('Runtime'),
                path: `/projects/${projectId}/runtimes`,
                activePathPattern: /\/(runtimes|new_runtime)\/?/,
                icon: <IconFont type='runtime' kind='white' size={20} />,
            },
            {
                title: t('Job'),
                path: `/projects/${projectId}/jobs`,
                activePathPattern: /\/(jobs|new_job)\/?/,
                icon: <IconFont type='job' kind='white' size={20} />,
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
