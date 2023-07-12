import { fetchSignPathPrefix } from '@/domain/base/services/filestore'
import { useRef } from 'react'

function useSign() {
    const signPrefix = useRef<string>('')

    return {
        getSign: async () => {
            if (!signPrefix.current) {
                signPrefix.current = await fetchSignPathPrefix()
            }

            return signPrefix.current
        },
        resetSign: () => {
            signPrefix.current = ''
        },
        signPrefix: signPrefix.current,
    }
}

export { useSign }
