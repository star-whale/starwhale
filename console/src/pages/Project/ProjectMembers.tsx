import React, { useState, useEffect } from 'react'
import { useFetchProjectMembers } from '@project/hooks/useFetchProjectMembers'
import { useParams } from 'react-router-dom'
import Button from '@/components/Button'
import Card from '@/components/Card'
import IconFont from '@/components/IconFont'
import { SIZE as ButtonSize } from 'baseui/button'
import { QueryInput } from '@/components/data-table/stateful-data-table'
import Table from '@/components/Table'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { useStyletron } from 'baseui'
import { IProjectRoleSchema } from '@project/schemas/project'
import RoleSelector from '@project/components/RoleSelector'
import { changeProjectRole, addProjectRole, removeProjectRole } from '@project/services/project'
import { toaster } from 'baseui/toast'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import MemberAddForm from '@project/components/MemberAddForm'
import { ConfirmButton } from '@/components/Modal/confirm'

export default function ProjectMembers() {
    const { projectId } = useParams<{ projectId: string }>()
    const members = useFetchProjectMembers(projectId)
    const [t] = useTranslation()
    const [css] = useStyletron()
    const [data, setData] = useState<IProjectRoleSchema[]>([])
    const [filter, setFilter] = useState('')
    const [showAddMember, setShowAddMember] = useState(false)

    useEffect(() => {
        const items = members.data ?? []
        setData(items.filter((i) => (filter && i.user.name.includes(filter)) || filter === ''))
    }, [filter, members.data])

    return (
        <Card
            title={t('Manage Project Members')}
            extra={
                <Button
                    startEnhancer={<IconFont type='add' kind='white' />}
                    size={ButtonSize.compact}
                    onClick={() => {
                        setShowAddMember(true)
                    }}
                >
                    {t('Add Project Member')}
                </Button>
            }
        >
            <div className={css({ marginBottom: '20px', width: '280px' })}>
                <QueryInput
                    onChange={(val: string) => {
                        setFilter(val.trim())
                    }}
                />
            </div>
            <Table
                isLoading={members.isLoading}
                columns={[t('sth name', [t('User')]), t('Project Role'), t('Created'), t('Action')]}
                data={
                    data.map(({ id, user, role }) => [
                        user.name,
                        <div style={{ maxWidth: '200px', padding: '5px 0 5px 0' }} key={id}>
                            <RoleSelector
                                value={role.id}
                                onChange={async (roleId) => {
                                    await changeProjectRole(projectId, id, roleId)
                                    toaster.positive(t('Change project role success'), { autoHideDuration: 1000 })
                                    await members.refetch()
                                }}
                            />
                        </div>,
                        user.createdTime && formatTimestampDateTime(user.createdTime),
                        <ConfirmButton
                            as='link'
                            key={id}
                            title={t('Remove Project Role Confirm')}
                            onClick={async () => {
                                await removeProjectRole(projectId, id)
                                toaster.positive(t('Remove Project Role Success'), { autoHideDuration: 1000 })
                                await members.refetch()
                            }}
                        >
                            {t('Remove Project Member')}
                        </ConfirmButton>,
                    ]) ?? []
                }
            />
            <Modal closeable isOpen={showAddMember} onClose={() => setShowAddMember(false)}>
                <ModalHeader>{t('Add Project Member')}</ModalHeader>
                <ModalBody>
                    <MemberAddForm
                        users={data.map(({ user }: IProjectRoleSchema) => user)}
                        onSubmit={async (info) => {
                            await addProjectRole(projectId, info.userId, info.roleId)
                            toaster.positive(t('Add project role success'), { autoHideDuration: 1000 })
                            setShowAddMember(false)
                            await members.refetch()
                        }}
                    />
                </ModalBody>
            </Modal>
        </Card>
    )
}
