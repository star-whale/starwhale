import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import IconFont from '@/components/IconFont'

export default function AdminSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()

    const navItems: INavItem[] = useMemo(() => {
        return [
            {
                title: t('Manage Users'),
                path: '/admin/users',
                icon: <IconFont type='user' />,
            },
            {
                title: t('System Settings'),
                path: '/admin/settings',
                icon: <IconFont type='setting' />,
            },
        ]
    }, [t])

    return (
        <BaseSidebar
            navItems={navItems}
            style={style}
            title={t('Admin Settings')}
            titleLink='/admin'
            icon={<IconFont type='setting2' style={{ color: 'white' }} />}
        />
    )
}
