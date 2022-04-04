import useGlobalState from '@/hooks/global'

export const useDatasetVersion = () => {
    const [datasetVerson, setDatasetVersion] = useGlobalState('datasetVersion')

    return {
        datasetVerson,
        setDatasetVersion,
    }
}

export const useDatasetVersionLoading = () => {
    const [datasetVersionLoading, setDatasetVersionLoading] = useGlobalState('datasetVersionLoading')

    return {
        datasetVersionLoading,
        setDatasetVersionLoading,
    }
}
