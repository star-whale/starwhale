import useGlobalState from '@/hooks/global'

export const useRuntimeVersion = () => {
    const [runtimeVerson, setRuntimeVersion] = useGlobalState('runtimeVersion')

    return {
        runtimeVerson,
        setRuntimeVersion,
    }
}

export const useRuntimeVersionLoading = () => {
    const [runtimeVersionLoading, setRuntimeVersionLoading] = useGlobalState('runtimeVersionLoading')

    return {
        runtimeVersionLoading,
        setRuntimeVersionLoading,
    }
}
