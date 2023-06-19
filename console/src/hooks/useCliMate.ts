import { useCallback, useEffect, useState } from 'react'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { cliMateServer } from '@/consts'

interface IPullParams {
    resourceUri: string
}

export default function useCliMate() {
    const [hasCliMate, setHasCliMate] = useState(false)

    useEffect(() => {
        axios
            .get(`${cliMateServer}/alive`)
            .then(() => {
                setHasCliMate(true)
            })
            .catch(() => {})
    })

    const doPull = useCallback(({ resourceUri }: IPullParams) => {
        axios
            .post(`${cliMateServer}/resource`, {
                url: `${window.location.protocol}//${window.location.host}/${resourceUri}`,
                token: localStorage.getItem('token'),
            })
            .then((res) => {
                const { data } = res
                toaster.positive(data.message, { autoHideDuration: 2000 })
            })
            .catch((e) => toaster.negative(e.message, { autoHideDuration: 2000 }))
    }, [])

    return { hasCliMate, doPull }
}
