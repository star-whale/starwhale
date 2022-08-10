import React from 'react'
import { Breadcrumbs } from 'baseui/breadcrumbs'
import { useHistory } from 'react-router-dom'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { createUseStyles } from 'react-jss'

const useMainStyles = createUseStyles({
    mainWrapper: {
        display: 'flex',
        flexFlow: 'row nowrap',
        justifyContent: 'space-between',
        position: 'relative',
        flex: '1',
    },
    mainContentWrapper: {
        overflowY: 'auto',
        height: '100%',
        flexGrow: 1,
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
    },
    mainContent: {
        padding: '12px 20px',
        border: '8px',
        boxSizing: 'border-box',
        minWidth: '792px',
        display: 'flex',
        flexDirection: 'column',
        flexGrow: 1,
        // paddingBottom: '30px',
    },
})

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
    const styles = useMainStyles()

    return (
        <main
            className={styles.mainWrapper}
            style={{
                ...style,
            }}
        >
            {Sidebar && <Sidebar />}
            <div className={styles.mainContentWrapper}>
                <div
                    className={styles.mainContent}
                    style={{
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
                                                    {Icon}
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
