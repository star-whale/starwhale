import React, { useEffect, useState } from 'react'
import axios from 'axios'
import { cliMateServer } from '@/consts'
import { useAuth } from '@/api/Auth'
import useTranslation from '@/hooks/useTranslation'
import { Spinner } from 'baseui/spinner'

export default function ClientLogin() {
    const [t] = useTranslation()
    const { token } = useAuth()
    const [success, setSuccess] = useState(false)
    const [loading, setLoading] = useState(false)
    const [errMsg, setErrMsg] = useState('')
    useEffect(() => {
        if (!token) {
            return
        }
        setLoading(true)
        axios
            .get(`${cliMateServer}/login`, { params: { token } })
            .then(({ data: { message } }) => {
                if (message !== 'success') {
                    setErrMsg(message)
                } else {
                    setSuccess(true)
                }
                setLoading(false)
            })
            .catch(() => {
                setErrMsg(t('Client Login Failed'))
                setLoading(false)
            })
    }, [])
    return <div>{loading ? <Spinner /> : <div>{success ? t('Client Login Success') : errMsg}</div>}</div>
}
