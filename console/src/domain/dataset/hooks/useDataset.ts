import useGlobalState from '@/hooks/global'

export const useDataset = () => {
    const [dataset, setDataset] = useGlobalState('dataset')

    return {
        dataset,
        setDataset,
    }
}

export const useDatasetLoading = () => {
    const [datasetLoading, setDatasetLoading] = useGlobalState('datasetLoading')

    return {
        datasetLoading,
        setDatasetLoading,
    }
}
