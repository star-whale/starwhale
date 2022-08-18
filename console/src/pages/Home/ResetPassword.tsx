import React, { useCallback } from 'react'
import LoginLayout from '@/pages/Home/LoginLayout'
import ResetPasswordForm from '@user/components/ResetPasswordForm'
import { useSearchParam } from 'react-use'
import { resetPassword } from '@user/services/user'
import useTranslation from '@/hooks/useTranslation'
import { toaster } from 'baseui/toast'
import { useHistory } from 'react-router-dom'

export default function ResetPassword() {
    const [t] = useTranslation()
    const history = useHistory()

    const title = useSearchParam('title') ?? ''
    const verification = useSearchParam('verification') ?? ''

    const handleSubmit = useCallback(
        async (password: string) => {
            await resetPassword(password, verification)
            toaster.positive(t('Reset Password Success'), { autoHideDuration: 3000 })
            history.push('/')
        },
        [verification, t, history]
    )

    return (
        <LoginLayout>
            <ResetPasswordForm title={title} onSubmit={handleSubmit} />
        </LoginLayout>
    )
}
