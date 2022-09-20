import useGlobalState from '@/hooks/global'

export const useProjectRole = () => {
    const [role, setRole] = useGlobalState('role')

    return {
        role,
        setRole,
    }
}
