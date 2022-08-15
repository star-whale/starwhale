import React from 'react'
import LoginLayout from '@/pages/Home/LoginLayout'
import ResetPasswordForm from '@user/components/ResetPasswordForm'

export default function ResetPassword() {
    return (
        <LoginLayout>
            <ResetPasswordForm onSubmit={() => {}} />
        </LoginLayout>
    )
}
