import React, { useState, useEffect } from 'react'
import { useHistory } from 'react-router-dom'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { StyledLink } from 'baseui/link'
import Card from '@/components/Card'
import IconFont from '@/components/IconFont'
import Table from '@/components/Table'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useFetchUsers } from '@user/hooks/useUser'
import { QueryInput } from '@/components/data-table/stateful-data-table'
import { useStyletron } from 'baseui'
import { IUserSchema } from '@user/schemas/user'
import { changeUserState } from '@user/services/user'
import { toaster } from 'baseui/toast'

interface IActionProps {
    title: string
    marginRight?: boolean
    onClick: () => Promise<void>
}

const ActionButton: React.FC<IActionProps> = ({ title, marginRight = false, onClick }: IActionProps) => {
    const style = {
        textDecoration: 'none',
        marginRight: marginRight ? '10px' : '0px',
    }

    return (
        <StyledLink style={style} onClick={onClick}>
            {title}
        </StyledLink>
    )
}

export default function UserManagement() {
    const [page] = usePage()
    const [t] = useTranslation()
    const users = useFetchUsers(page)
    const history = useHistory()
    const [css] = useStyletron()
    const [data, updateData] = useState<IUserSchema[]>([])
    const [filter, updateFilter] = useState('')

    useEffect(() => {
        const items = users.data?.list ?? []
        updateData(items.filter((i) => (filter && i.name.includes(filter)) || filter === ''))
    }, [filter, users.data])

    const changUserState = async (userId: string, enable: boolean): Promise<void> => {
        await changeUserState(userId, enable)
        toaster.positive(enable ? t('Enable User Success') : t('Disable User Success'), { autoHideDuration: 1000 })
        await users.refetch()
        return Promise.resolve()
    }

    return (
        <Card
            title={t('Manage Users')}
            extra={
                <Button
                    startEnhancer={<IconFont type='add' kind='white' />}
                    size={ButtonSize.compact}
                    onClick={() => history.push('new_job')}
                >
                    {t('Add User')}
                </Button>
            }
        >
            <div className={css({ marginBottom: '20px' })}>
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
                        user.isEnabled ? t('Enabled User') : t('Disabled User'),
                        user.createdTime && formatTimestampDateTime(user.createdTime),
                        <div key={user.id}>
                            <ActionButton
                                marginRight
                                title={user.isEnabled ? t('Disable User') : t('Enable User')}
                                onClick={() => changUserState(user.id, !user.isEnabled)}
                            />
                            &nbsp; {/* make segmenter works well when double click */}
                            <ActionButton title={t('Change Password')} onClick={async (): Promise<void> => {}} />
                        </div>,
                    ]) ?? []
                }
            />
        </Card>
    )
}
