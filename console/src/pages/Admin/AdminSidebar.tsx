import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { AiOutlineSetting, AiOutlineUser } from 'react-icons/ai'

export default function AdminSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()

    const navItems: INavItem[] = useMemo(() => {
        return [
            {
                title: t('Manage Users'),
                path: '/admin/users',
                icon: <AiOutlineUser size={20} />,
            },
        ]
    }, [t])

    return (
        <BaseSidebar
            navItems={navItems}
            style={style}
            title={t('Admin Settings')}
            titleLink='/admin'
            icon={<AiOutlineSetting style={{ color: '#fff' }} size={20} />}
        />
    )
}
