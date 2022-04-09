import useGlobalState from '@/hooks/global'

export const useJob = () => {
    const [job, setJob] = useGlobalState('job')

    return {
        job,
        setJob,
    }
}

export const useJobLoading = () => {
    const [jobLoading, setJobLoading] = useGlobalState('jobLoading')

    return {
        jobLoading,
        setJobLoading,
    }
}
