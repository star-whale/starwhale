import { headerHeight } from '@/consts'
import React from 'react'
import { Breadcrumbs } from 'baseui/breadcrumbs'
import { useHistory } from 'react-router-dom'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@/theme'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { useStyletron } from 'baseui'

const useStyles = ({ theme, themeType }: IThemedStyleProps) =>
    createUseStyles({
        root: {
            display: 'flex',
            flexDirection: 'column',
            zIndex: 1,
            minHeight: '100vh',
            // backgroundColor: theme.palette.background.default,
            position: 'relative',
            minWidth: 'fit-content',
            width: '100%',
            // color: theme.palette.getContrastText(theme.palette.background.default),
        },
        // appFrame: {
        //     display: 'flex',
        //     flexDirection: 'column',
        //     flexGrow: 1,
        //     [theme.breakpoints.up('xs')]: {
        //         marginTop: theme.spacing(6),
        //     },
        //     [theme.breakpoints.down('xs')]: {
        //         marginTop: theme.spacing(7),
        //     },
        // },
        // contentWithSidebar: {
        //     display: 'flex',
        //     flexGrow: 1,
        // },
        // content: {
        //     backgroundColor: theme.palette.background.default,
        //     zIndex: 2,
        //     display: 'flex',
        //     flexDirection: 'column',
        //     flexGrow: 1,
        //     flexBasis: 0,
        //     padding: theme.spacing(3),
        //     paddingTop: theme.spacing(1),
        //     paddingLeft: 0,
        //     [theme.breakpoints.up('xs')]: {
        //         paddingLeft: 5,
        //     },
        //     [theme.breakpoints.down('sm')]: {
        //         padding: 0,
        //     },
        // },
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
    const themeType = useCurrentThemeType()
    const [, theme] = useStyletron()
    const styles = useStyles({ themeType, theme })()
    return (
        <main
            style={{
                display: 'flex',
                flexFlow: 'row nowrap',
                justifyContent: 'space-between',
                minHeight: '100vh',
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
                        padding: '48px',
                        height: '100%',
                        boxSizing: 'border-box',
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
