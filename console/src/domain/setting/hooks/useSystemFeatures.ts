import useGlobalState from '@/hooks/global'

export const useSystemFeatures = () => {
    const [systemFeatures, setSystemFeatures] = useGlobalState('systemFeatures')
    return {
        systemFeatures,
        setSystemFeatures,
    }
}
