import useGlobalState from '@/hooks/global'

export const useModelVersion = () => {
    const [modelVerson, setModelVersion] = useGlobalState('modelVersion')

    return {
        modelVerson,
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
