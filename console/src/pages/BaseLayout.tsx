import React from 'react'
import { Breadcrumbs } from 'baseui/breadcrumbs'
import { useHistory } from 'react-router-dom'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import Header from '@/components/Header'
import { headerHeight } from '@/consts'

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

    return (
        <main
            style={{
                display: 'flex',
                flexFlow: 'row nowrap',
                justifyContent: 'space-between',
                height: '100vh',
                width: '100vw',
                position: 'relative',
                ...style,
            }}
        >
            {Sidebar && <Sidebar />}
            <div
                style={{
                    overflowY: 'auto',
                    height: '100%',
                    flexGrow: 1,
                    position: 'relative',
                }}
            >
                <Header />
                <div
                    style={{
                        padding: '28px',
                        border: '8px',
                        boxSizing: 'border-box',
                        minWidth: '792px',
                        display: 'flex',
                        flexDirection: 'column',
                        paddingTop: !breadcrumbItems ? headerHeight : '28px',
                        ...contentStyle,
                    }}
                >
                    {(breadcrumbItems || extra) && (
                        <div style={{ marginBottom: 27, display: 'flex', alignItems: 'center' }}>
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
