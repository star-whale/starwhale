import { useQuery } from 'react-query'
import { fetchRuntimeVersionSuggestion } from '../services/runtimeVersion'

export function useFetchRuntimeVersionSuggestion(projectId: string, modelId: string) {
    return useQuery(
        ['fetchRuntimeVersionSuggestion', projectId, modelId],
        () => fetchRuntimeVersionSuggestion(projectId, modelId),
        {
            refetchOnWindowFocus: false,
        }
    )
}
