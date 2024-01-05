import React, { useEffect, useState } from 'react'
import axios from 'axios'
import { cliMateServer } from '@/consts'
import { useAuth } from '@/api/Auth'
import useTranslation from '@/hooks/useTranslation'
// eslint-disable-next-line
import { Spinner } from 'baseui/spinner'
import { useSearchParam } from 'react-use'
import Button from '@starwhale/ui/Button'

export default function ClientLogin() {
    const random = useSearchParam('random')

    const [t] = useTranslation()
    const { token } = useAuth()
    const [success, setSuccess] = useState(false)
    const [loading, setLoading] = useState(false)
    const [errMsg, setErrMsg] = useState('')

    const [permited, setPermited] = useState(false)

    useEffect(() => {
        if (!token || !permited) {
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
    }, [permited, t, token])
    return (
        <div className='flex justify-center'>
            {permited ? null : (
                <div className='flex flex-col items-center'>
                    <p>{t('Client Login Confirm', [random])}</p>
                    <div className='m-3'>
                        <Button onClick={() => setPermited(true)}>{t('Allow')}</Button>
                    </div>
                </div>
            )}
            {loading ? <Spinner /> : <div>{success ? t('Client Login Success') : errMsg}</div>}
        </div>
    )
}
