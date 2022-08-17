import React, { useState } from 'react'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import useTranslation from '@/hooks/useTranslation'
import IconFont from '@/components/IconFont'
import Button from '@/components/Button'

export interface IEmailConfirmProps {
    show: boolean
    email?: string
    alreadyVerified: () => Promise<void>
    resendEmail: () => Promise<void>
}

export default function EmailConfirm({ show, email, alreadyVerified, resendEmail }: IEmailConfirmProps) {
    const [t] = useTranslation()
    const [resendLoading, setResendLoading] = useState(false)
    const [verifiedLoading, setVerifiedLoading] = useState(false)

    return (
        <Modal isOpen={show} closeable={false}>
            <ModalHeader>
                <IconFont type='warning' kind='gray' style={{ marginRight: '10px' }} />
                {t('Check Your Email')}
            </ModalHeader>
            <ModalBody>
                <p>{t('Please verify the email send from Starwhale')}</p>
                <p>{email}</p>
                <div style={{ display: 'flex', justifyContent: 'space-around', marginTop: '30px' }}>
                    <Button
                        kind='secondary'
                        isLoading={resendLoading}
                        onClick={() => {
                            setResendLoading(true)
                            resendEmail().finally(() => {
                                setResendLoading(false)
                            })
                        }}
                    >
                        {t('Resend Email')}
                    </Button>
                    <Button
                        isLoading={verifiedLoading}
                        onClick={() => {
                            setVerifiedLoading(true)
                            alreadyVerified().finally(() => {
                                setVerifiedLoading(false)
                            })
                        }}
                    >
                        {t('Already Verified')}
                    </Button>
                </div>
            </ModalBody>
        </Modal>
    )
}
