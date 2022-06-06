import useGlobalState from '@/hooks/global'

export const useRuntime = () => {
    const [runtime, setRuntime] = useGlobalState('runtime')

    return {
        runtime,
        setRuntime,
    }
}

export const useRuntimeLoading = () => {
    const [runtimeLoading, setRuntimeLoading] = useGlobalState('runtimeLoading')

    return {
        runtimeLoading,
        setRuntimeLoading,
    }
}
