import React from 'react'
import { Tabs, Tab, TabsProps, TabProps } from 'baseui/tabs'
import { useHistory } from 'react-router-dom'
import { StatefulTooltip } from 'baseui/tooltip'
import { mergeOverrides } from '@/utils/baseui'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { INavItem } from './BaseSidebar'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'
import { expandBorder, expandPadding } from '@starwhale/ui/utils'

export interface IComposedNavTabsProps {
    // style?: React.CSSProperties
    // navStyle?: React.CSSProperties
    // fill?: TabsProps['fill']
    tabsOverrides?: TabsProps['overrides']
    tabOverrides?: TabProps['overrides']
    align?: 'left' | 'center'
}

export interface IBaseNavTabsProps extends IComposedNavTabsProps {
    navItems: INavItem[]
}

export function BaseSimpleNavTabs({
    navItems,
    // fill = 'intrinsic',
    tabsOverrides,
    tabOverrides,
    align = 'left',
}: IBaseNavTabsProps) {
    const history = useHistory()
    const [, theme] = themedUseStyletron()
    const { activeItemPath } = useRouterActivePath(navItems)
    if (!navItems || navItems.length === 0) {
        return null
    }

    return (
        <Tabs
            activeKey={activeItemPath}
            onChange={({ activeKey }) => {
                history.push(activeKey as string)
            }}
            // fill={fill}
            // activateOnFocus
            overrides={mergeOverrides(
                {
                    TabBar: {
                        style: {
                            backgroundColor: 'none',
                            paddingLeft: 0,
                        },
                    },
                    Tab: {
                        props: {
                            className:
                                'first:rounded-tl-4px  first:rounded-bl-4px last:rounded-tr-4px last:rounded-br-4px  border-1px',
                        },
                        style: ({ $active }) => {
                            return {
                                position: 'relative',
                                zIndex: 1,
                                // marginBottom: '-1px',
                                // borderBottom: $active ? '0px' : '1px solid #E2E7F0',
                                backgroundColor: $active ? '#fff' : '#F7F8FA;',
                                color: $active ? '#2B65D9' : 'rgba(2,16,43,0.60)',
                                textAlign: 'center',
                                paddingTop: '9px',
                                paddingBottom: '9px',
                                lineHeight: '1',
                                marginLeft: 0,
                                marginRight: 0,
                                paddingLeft: '14px',
                                paddingRight: '14px',
                                fontSize: '14px',
                            }
                        },
                    },
                    Root: {
                        style: {
                            height: '100%',
                            flex: 1,
                            flexDirection: 'column',
                            position: 'relative',
                        },
                    },
                    TabContent: {
                        style: () => {
                            return {
                                flex: 1,
                                flexDirection: 'column',
                                position: 'relative',
                                overflow: 'auto',
                                ...expandBorder('1px', 'solid', '#E2E7F0'),
                                ...expandPadding('20px', '20px', '20px', '20px'),
                                display: 'none',
                            }
                        },
                    },
                    ...(align === 'center'
                        ? {
                              TabList: {
                                  style: {
                                      justifyContent: 'center',
                                  },
                              },
                          }
                        : {}),
                },
                tabsOverrides
            )}
        >
            {navItems.map((item) => {
                const Icon = item.icon
                return (
                    <Tab
                        overrides={mergeOverrides(
                            {
                                TabPanel: {
                                    style: {
                                        paddingLeft: '0',
                                        paddingRight: '0',
                                        paddingBottom: '0',
                                        paddingTop: 0,
                                        display: 'none',
                                    },
                                },
                                Tab: {
                                    style: {
                                        'backgroundColor': 'transparent',
                                        'color':
                                            item.path === activeItemPath ? theme.brandPrimary : 'rgba(2,16,43,0.60);',
                                        ':hover': {
                                            backgroundColor: 'transparent',
                                            color: theme.brandPrimary,
                                        },
                                        ...(item.path === activeItemPath
                                            ? {
                                                  position: 'relative',
                                                  zIndex: 1,
                                                  borderBottom: '1px solid #2B65D9',
                                                  borderTop: '1px solid #2B65D9',
                                                  borderLeft: '1px solid #2B65D9',
                                                  borderRight: '1px solid #2B65D9',
                                                  fontWeight: '600',
                                              }
                                            : {
                                                  borderBottom: '1px solid #E2E7F0',
                                                  borderTop: '1px solid #E2E7F0',
                                                  borderLeft: '1px solid #E2E7F0',
                                                  borderRight: '1px solid #E2E7F0',
                                              }),
                                    },
                                },
                            },
                            tabOverrides
                        )}
                        disabled={item.disabled}
                        key={item.path}
                        title={
                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 12,
                                    lineHeight: '1',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                    overflow: 'hidden',
                                }}
                            >
                                {Icon}
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 6,
                                    }}
                                >
                                    <span>{item.title}</span>
                                    {item.helpMessage && (
                                        <StatefulTooltip content={item.helpMessage} showArrow>
                                            <div
                                                style={{
                                                    display: 'inline-flex',
                                                    cursor: 'pointer',
                                                }}
                                            />
                                        </StatefulTooltip>
                                    )}
                                </div>
                            </div>
                        }
                    />
                )
            })}
        </Tabs>
    )
}
