import React, { useContext } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { createUseStyles } from 'react-jss'
import { SidebarContext } from '@/contexts/SidebarContext'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import Logo from './Logo'
import LanguageSelector from './LanguageSelector'
import { docsEN, docsZH, headerHeight } from '@/consts'
import i18n from '@/i18n'

const useHeaderStyles = createUseStyles({
    headerWrapper: (props: IThemedStyleProps) => ({
        padding: '0 20px 0 0',
        height: `${headerHeight}px`,
        width: '100%',
        display: 'flex',
        flexFlow: 'row nowrap',
        alignItems: 'center',
        color: props.theme.colors.contentPrimary,
        backgroundColor: props.theme.brandBgNav,
        position: 'relative',
        zIndex: 10,
    }),
})

export default function HeaderSimple() {
    const [, theme] = themedUseStyletron()
    const headerStyles = useHeaderStyles({ theme })
    const ctx = useContext(SidebarContext)
    const [t] = useTranslation()

    const isZH = i18n.language === 'zh'

    return (
        <header className={headerStyles.headerWrapper}>
            <Logo expanded={ctx.expanded} />
            <div style={{ flexGrow: 1 }} />
            <div
                style={{
                    flexShrink: 0,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    marginRight: '16px',
                }}
            >
                <LanguageSelector />
                <a
                    target='_blank'
                    rel='noreferrer'
                    href={isZH ? docsZH : docsEN}
                    style={{ color: '#FFF', textDecoration: 'none', lineHeight: '20px' }}
                >
                    {t('docs')}
                </a>
            </div>
        </header>
    )
}
