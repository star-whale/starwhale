import useGlobalState from '@/hooks/global'

export const useProject = () => {
    const [project, setProject] = useGlobalState('project')

    return {
        project,
        setProject,
    }
}

export const useProjectLoading = () => {
    const [projectLoading, setProjectLoading] = useGlobalState('projectLoading')

    return {
        projectLoading,
        setProjectLoading,
    }
}
