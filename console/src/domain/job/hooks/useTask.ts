import useGlobalState from '@/hooks/global'

export const useTask = () => {
    const [jobVerson, setTask] = useGlobalState('task')

    return {
        jobVerson,
        setTask,
    }
}

export const useTaskLoading = () => {
    const [taskLoading, setTaskLoading] = useGlobalState('taskLoading')

    return {
        taskLoading,
        setTaskLoading,
    }
}
