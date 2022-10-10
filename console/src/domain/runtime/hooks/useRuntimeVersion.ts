import useGlobalState from '@/hooks/global'

export const useRuntimeVersion = () => {
    const [runtimeVersion, setRuntimeVersion] = useGlobalState('runtimeVersion')

    return {
        runtimeVersion,
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
