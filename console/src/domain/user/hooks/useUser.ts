import { useQuery } from 'react-query'
import qs from 'qs'
import useGlobalState from '@/hooks/global'
import { IListQuerySchema } from '@base/schemas/list'
import { listUsers } from '@user/services/user'

export const useUser = () => {
    const [user, setUser] = useGlobalState('user')

    return {
        user,
        setUser,
    }
}

export const useUserLoading = () => {
    const [userLoading, setUserLoading] = useGlobalState('userLoading')

    return {
        userLoading,
        setUserLoading,
    }
}

export function useFetchUsers(query: IListQuerySchema) {
    return useQuery(['fetch users', qs.stringify(query)], () => listUsers(query), { refetchOnWindowFocus: false })
}
