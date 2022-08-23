import useGlobalState from '@/hooks/global'

export const useDatasetVersion = () => {
    const [datasetVersion, setDatasetVersion] = useGlobalState('datasetVersion')

    return {
        datasetVersion,
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
