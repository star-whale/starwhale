import { headerHeight } from '@/consts'
import React from 'react'
import { Breadcrumbs } from 'baseui/breadcrumbs'
import { useHistory } from 'react-router-dom'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@/theme'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { useStyletron } from 'baseui'

export interface IBaseLayoutProps {
    children: React.ReactNode
    breadcrumbItems?: INavItem[]
    extra?: React.ReactNode
    sidebar?: React.ComponentType<IComposedSidebarProps>
    contentStyle?: React.CSSProperties
    style?: React.CSSProperties
}

export default function BaseLayout({
    breadcrumbItems,
    extra,
    children,
    sidebar: Sidebar,
    style,
    contentStyle,
}: IBaseLayoutProps) {
    const history = useHistory()
    const themeType = useCurrentThemeType()
    const [, theme] = useStyletron()

    return (
        <main
            style={{
                display: 'flex',
                flexFlow: 'row nowrap',
                justifyContent: 'space-between',
                height: '100vh',
                position: 'relative',
                ...style,
            }}
        >
            {Sidebar && <Sidebar style={{ marginTop: headerHeight }} />}
            <div
                style={{
                    overflowY: 'auto',
                    paddingTop: headerHeight,
                    flexGrow: 1,
                }}
            >
                <div
                    style={{
                        padding: '32px',
                        height: '100%',
                        boxSizing: 'border-box',
                        minWidth: '792px',
                        display: 'flex',
                        flexDirection: 'column',
                        ...contentStyle,
                    }}
                >
                    {(breadcrumbItems || extra) && (
                        <div style={{ marginBottom: 18, display: 'flex', alignItems: 'center' }}>
                            {breadcrumbItems && (
                                <div style={{ flexShrink: 0 }}>
                                    <Breadcrumbs
                                        overrides={{
                                            List: {
                                                style: {
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                },
                                            },
                                            ListItem: {
                                                style: {
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                },
                                            },
                                        }}
                                    >
                                        {breadcrumbItems.map((item, idx) => {
                                            const Icon = item.icon
                                            return (
                                                <div
                                                    role='button'
                                                    tabIndex={0}
                                                    style={{
                                                        fontSize: '13px',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        gap: 6,
                                                        cursor:
                                                            idx !== breadcrumbItems.length - 1 ? 'pointer' : undefined,
                                                    }}
                                                    key={item.path}
                                                    onClick={
                                                        item.path && idx !== breadcrumbItems.length - 1
                                                            ? () => {
                                                                  if (item.path) {
                                                                      history.push(item.path)
                                                                  }
                                                              }
                                                            : undefined
                                                    }
                                                >
                                                    {Icon && <Icon size={12} />}
                                                    <span>{item.title}</span>
                                                </div>
                                            )
                                        })}
                                    </Breadcrumbs>
                                </div>
                            )}
                            <div style={{ flexGrow: 1 }} />
                            <div style={{ flexShrink: 0 }}>{extra}</div>
                        </div>
                    )}
                    {children}
                </div>
            </div>
        </main>
    )
}
