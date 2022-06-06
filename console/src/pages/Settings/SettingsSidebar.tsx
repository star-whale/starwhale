import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { AiOutlineSetting, AiOutlineCloudServer } from 'react-icons/ai'

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
            icon={<AiOutlineSetting style={{ color: '#fff' }} size={20} />}
        />
    )
}
