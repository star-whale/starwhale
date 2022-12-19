import useGlobalState from '@/hooks/global'

export const useModelVersion = () => {
    const [modelVersion, setModelVersion] = useGlobalState('modelVersion')

    return {
        modelVersion,
        setModelVersion,
    }
}

export const useModelVersionLoading = () => {
    const [modelVersionLoading, setModelVersionLoading] = useGlobalState('modelVersionLoading')

    return {
        modelVersionLoading,
        setModelVersionLoading,
    }
}
