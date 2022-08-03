import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { AiOutlineCloudServer } from 'react-icons/ai'
import IconFont from '@/components/IconFont'

export default function SettingsSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const name = t('SETTINGS')

    const navItems: INavItem[] = useMemo(() => {
        return [
            {
                title: t('Agent'),
                path: '/settings/agents',
                activePathPattern: /\/(agents)\/?/,
                icon: <AiOutlineCloudServer size={20} />,
            },
        ]
    }, [t])

    return (
        <BaseSidebar
            navItems={navItems}
            style={style}
            title={name}
            titleLink='/settings'
            icon={<IconFont type='setting' style={{ color: 'white' }} />}
        />
    )
}
