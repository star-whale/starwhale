import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import BaseSidebar, { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import IconFont from '@/components/IconFont'
import { AiOutlineSetting, AiOutlineCloudServer } from 'react-icons/ai'
import { MdSettingsSystemDaydream } from 'react-icons/md'
import { GrDocker } from 'react-icons/gr'

export default function SettingsSidebar({ style }: IComposedSidebarProps) {
    const [t] = useTranslation()
    const name = t('SETTINGS')

    const navItems: INavItem[] = useMemo(() => {
        return [
            {
                title: t('BaseImage'),
                path: `/settings/images`,
                activePathPattern: /\/(images)\/?/,
                icon: <GrDocker size={20} />,
            },
            {
                title: t('Agent'),
                path: `/settings/agents`,
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
            titleLink={'/settings'}
            icon={<AiOutlineSetting style={{ color: '#fff' }} size={20} />}
        />
    )
}
