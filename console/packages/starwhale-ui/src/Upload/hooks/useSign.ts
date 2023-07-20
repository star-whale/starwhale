import { fetchSignPathPrefix } from '@/domain/base/services/filestore'
import { useEffect, useRef } from 'react'

function useSign() {
    const signPrefix = useRef<string>('')

    const getSign = async () => {
        if (!signPrefix.current) {
            signPrefix.current = await fetchSignPathPrefix()
        }

        return signPrefix.current
    }

    useEffect(() => {
        getSign()
    }, [])

    return {
        getSign,
        resetSign: async () => {
            signPrefix.current = await fetchSignPathPrefix()
        },
        signPrefix: signPrefix.current,
    }
}

export { useSign }
