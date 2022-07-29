import useGlobalState from '@/hooks/global'

export const useCurrentUserRoles = () => useGlobalState('currentUserRoles')
