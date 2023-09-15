import React, { useState, useEffect } from 'react'
import Card from '@/components/Card'
import Table from '@/components/Table'
import Button, { ButtonGroup, ExtendButton } from '@starwhale/ui/Button'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useFetchUsers } from '@user/hooks/useUser'
import { useStyletron } from 'baseui'
import { IUserSchema } from '@user/schemas/user'
import { changeUserState, createUser, changeUserPasswd } from '@user/services/user'
import { toaster } from 'baseui/toast'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import NewUserForm from '@user/components/NewUserForm'
import generatePassword from '@/utils/passwordGenerator'
import Input, { QueryInput } from '@starwhale/ui/Input'
import CopyToClipboard from 'react-copy-to-clipboard'
import PasswordForm from '@user/components/PasswordForm'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { ConfirmButton } from '@starwhale/ui/Modal'
import { Toggle } from '@starwhale/ui/Select/Toggle'

interface IPasswordResultProps {
    title: string
    longTips: string
    password: string
}

export default function UserManagement() {
    const [page] = usePage()
    const [t] = useTranslation()
    const $page = React.useMemo(() => {
        return {
            ...page,
            pageSize: 99999,
        }
    }, [page])
    const users = useFetchUsers($page)
    const [css] = useStyletron()
    const [data, updateData] = useState<IUserSchema[]>([])
    const [filter, updateFilter] = useState('')
    const [showAddUser, setShowAddUser] = useState(false)
    const [passwordResult, setPasswordResult] = useState<IPasswordResultProps | undefined>()
    const [modifyingUser, setModifyingUser] = useState<IUserSchema | undefined>(undefined)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()

    useEffect(() => {
        const items = users.data?.list ?? []
        updateData(items.filter((i) => (filter && i.name.includes(filter)) || filter === ''))
    }, [filter, users.data])

    const changUserState = async (userId: string, enable: boolean) => {
        await changeUserState(userId, enable)
        toaster.positive(enable ? t('Enable User Success') : t('Disable User Success'), { autoHideDuration: 1000 })
        await users.refetch()
        return Promise.resolve()
    }

    const submitPasswd = async (create: boolean, userName: string, userPwd: string, originPwd: string) => {
        let pass = userPwd
        const useRandom = !pass
        if (useRandom) {
            // we generate password for the user
            pass = generatePassword()
        }
        if (create) {
            await createUser(userName, pass)
            setShowAddUser(false)
        } else {
            await changeUserPasswd(userName, originPwd, pass)
            setModifyingUser(undefined)
        }

        if (useRandom) {
            // show generated password after a while
            await setTimeout(() => {
                setPasswordResult({
                    title: create ? t('Add User Success') : t('Update User Success'),
                    longTips: create ? t('Random Password Tips For Add') : t('Random Password Tips For Update'),
                    password: pass,
                })
            }, 500)
        } else {
            const tip = create ? t('Add User Success') : t('Update User Success')
            toaster.positive(tip, { autoHideDuration: 1000 })
        }

        await users.refetch()
        return Promise.resolve()
    }

    return (
        <Card title={t('Manage Users')} extra={<Button onClick={() => setShowAddUser(true)}>{t('Add User')}</Button>}>
            <div className={css({ marginBottom: '20px', width: '280px' })}>
                <QueryInput
                    onChange={(val: string) => {
                        updateFilter(val.trim())
                    }}
                />
            </div>
            <Table
                isLoading={users.isLoading}
                columns={[t('sth name', [t('User')]), t('Status'), t('Created'), t('Action')]}
                data={
                    data.map((user) => [
                        user.name,
                        <div key='enable' className='status flex gap-9px items-center'>
                            <ConfirmButton
                                title={user.isEnabled ? t('Disable User Confirm') : t('Enable User Confirm')}
                                tooltip={user.isEnabled ? t('Disable User') : t('Enable User')}
                                as='link'
                                onClick={() => {
                                    changUserState(user.id, !user.isEnabled)
                                }}
                                disabled={user.id === currentUser?.id}
                                icondisable={user.id === currentUser?.id}
                            >
                                <Toggle
                                    value={Boolean(user.isEnabled) && user.id !== currentUser?.id}
                                    disabled={user.id === currentUser?.id}
                                />
                            </ConfirmButton>
                            {user.isEnabled ? t('Enabled User') : t('Disabled User')}
                        </div>,
                        user.createdTime && formatTimestampDateTime(user.createdTime),
                        <ButtonGroup key={user.id}>
                            <ExtendButton
                                tooltip={t('Change Password')}
                                icon='a-passwordresets'
                                as='link'
                                onClick={() => setModifyingUser(user)}
                            />
                        </ButtonGroup>,
                    ]) ?? []
                }
            />
            <Modal isOpen={showAddUser} closeable onClose={() => setShowAddUser(false)}>
                <ModalHeader>{t('Add User')}</ModalHeader>
                <ModalBody>
                    <NewUserForm
                        onSubmit={async ({ userName, userPwd }) => {
                            await submitPasswd(true, userName, userPwd, '')
                        }}
                    />
                </ModalBody>
            </Modal>
            <Modal animate closeable onClose={() => setPasswordResult(undefined)} isOpen={!!passwordResult}>
                <ModalHeader>{passwordResult?.title}</ModalHeader>
                <ModalBody>
                    <p>{passwordResult?.longTips}</p>
                    <div className={css({ display: 'flex', marginTop: '10px' })}>
                        <Input value={passwordResult?.password} />
                        <CopyToClipboard
                            text={passwordResult?.password ?? ''}
                            onCopy={() => {
                                toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                            }}
                        >
                            <Button>copy</Button>
                        </CopyToClipboard>
                    </div>
                </ModalBody>
            </Modal>
            <Modal isOpen={!!modifyingUser} onClose={() => setModifyingUser(undefined)} closeable animate autoFocus>
                <ModalHeader>{t('Change Password')}</ModalHeader>
                <hr />
                <ModalBody>
                    <PasswordForm
                        admin
                        currentUser={modifyingUser}
                        onSubmit={async ({ userPwd, originPwd }) => {
                            if (!modifyingUser) {
                                // unreachable, make eslint happy
                                return
                            }
                            await submitPasswd(false, modifyingUser.id, userPwd, originPwd)
                        }}
                    />
                </ModalBody>
            </Modal>
        </Card>
    )
}
