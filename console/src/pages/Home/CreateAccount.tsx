import React, { useCallback } from 'react'
import LoginLayout from '@/pages/Home/LoginLayout'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import CreateAccountForm from '@user/components/CreateAccountForm'
import useTranslation from '@/hooks/useTranslation'
import { useSearchParam } from 'react-use'
import { createAccount } from '@user/services/user'
import { setToken } from '@/api'
import { useHistory } from 'react-router-dom'

export default function CreateAccount() {
    const [t] = useTranslation()
    const title = useSearchParam('title') ?? ''
    const verification = useSearchParam('verification') ?? ''
    const history = useHistory()

    const handleSubmit = useCallback(
        async (name: string) => {
            const { token } = await createAccount(name, verification)
            setToken(token)
            // TODO redirect to the page before register
            history.push('/')
        },
        [verification, history]
    )

    return (
        <LoginLayout>
            <Modal isOpen closeable={false}>
                <ModalHeader>{t('Create Your Account')}</ModalHeader>
                <ModalBody>
                    {/* method from backend, e.g. github, google, a@b.c */}
                    <CreateAccountForm method={title} onSubmit={handleSubmit} />
                </ModalBody>
            </Modal>
        </LoginLayout>
    )
}
