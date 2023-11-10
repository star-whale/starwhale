import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { useParams } from 'react-router-dom'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import IconFont from '@starwhale/ui/IconFont'
import { useProjectRole } from '@/domain/project/hooks/useProjectRole'
import { useAuthPrivileged } from '@/api/WithAuth'

export default function ProjectSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const project = projectInfo?.data
    const projectName = project?.name || t('PROJECT')
    const { role } = useProjectRole()
    const { isPrivileged } = useAuthPrivileged({
        role,
        id: 'project.menu.trash',
    })

    const navItems: INavItem[] = useMemo(() => {
        if (!project) {
            return []
        }

        return [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/overview`,
                icon: <IconFont type='overview' size={16} />,
                activePathPattern: /\/(overview)\/?/,
            },
            {
                title: t('fine-tuning'),
                path: `/projects/${projectId}/sft-spaces`,
                icon: <IconFont type='overview' size={16} />,
                activePathPattern: /\/(sft-spaces)\/?/,
            },
            {
                title: t('Models'),
                path: `/projects/${projectId}/models`,
                icon: <IconFont type='Model' size={16} />,
                activePathPattern: /\/(models|online_eval)\/?/,
            },
            {
                title: t('Datasets'),
                path: `/projects/${projectId}/datasets`,
                activePathPattern: /\/(datasets|new_dataset)\/?/,
                icon: <IconFont type='dataset' size={16} />,
            },
            {
                title: t('Jobs'),
                path: `/projects/${projectId}/jobs`,
                activePathPattern: /\/(jobs|new_job)\/?/,
                icon: <IconFont type='job' size={16} />,
            },
            {
                title: t('Evaluations'),
                path: `/projects/${projectId}/evaluations`,
                activePathPattern: /\/(evaluations|new_job)\/?/,
                icon: <IconFont type='evaluation' size={16} />,
            },
            {
                title: t('Runtimes'),
                path: `/projects/${projectId}/runtimes`,
                activePathPattern: /\/(runtimes|new_runtime)\/?/,
                icon: <IconFont type='runtime' size={16} />,
            },
            {
                title: t('Reports'),
                path: `/projects/${projectId}/reports`,
                activePathPattern: /\/(reports)\/?/,
                icon: <IconFont type='report' size={16} />,
            },
            isPrivileged
                ? {
                      title: t('trash.title'),
                      path: `/projects/${projectId}/trashes`,
                      activePathPattern: /\/(trashes)\/?/,
                      icon: <IconFont type='delete' size={16} />,
                  }
                : undefined,
        ].filter((v) => !!v) as INavItem[]
    }, [project, projectId, t, isPrivileged])
    return (
        <BaseSidebar
            navItems={navItems}
            style={style}
            title={projectName}
            icon={<IconFont type='project' size={20} />}
        />
    )
}
