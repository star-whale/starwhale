import { api } from '@/api'
import useGlobalState from '@/hooks/global'
import { useParams } from 'react-router-dom'

export const useFineTune = () => {
    const [fineTune, setFineTune] = useGlobalState('fineTune')
    return {
        fineTune,
        setFineTune,
    }
}

export const useFineTuneLoading = () => {
    const [fineTuneLoading, setFineTuneLoading] = useGlobalState('fineTuneLoading')

    return {
        fineTuneLoading,
        setFineTuneLoading,
    }
}

export function useFineTuneInfo() {
    const { fineTune, setFineTune } = useFineTune()
    const { projectId, spaceId, fineTuneId } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)

    return {
        info,
        fineTune,
        setFineTune,
        params: {
            fineTuneId,
            projectId,
            spaceId,
        },
    }
}
