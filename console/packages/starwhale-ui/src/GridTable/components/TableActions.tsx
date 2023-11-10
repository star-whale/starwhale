import React from 'react'
import { ExtendButton } from '@starwhale/ui/Button'
import { PopoverContainer, SingleSelectMenu } from '@starwhale/ui/Popover'
import { expandPadding } from '@starwhale/ui/utils'
import { StatefulPopover } from 'baseui/popover'
import { useCreation } from 'ahooks'

const listOverrides = {
    ListItem: {
        style: {
            display: 'flex',
            justifyContent: 'start',
            ...expandPadding('0px', '0px', '0px', '0px'),
        },
    },
    List: {
        style: {
            minWidth: '110px',
        },
    },
}

const getPopoverOverrides = ({ left, top }) => {
    if (!left || !top) return {}

    return {
        Body: {
            props: {
                className: 'filter-popover',
                $popoverOffset: {
                    left,
                    top,
                },
            },
            style: {
                zIndex: 999,
            },
        },
    }
}

type TableActionsT =
    | {
          access?: boolean
          quickAccess?: boolean
          component: React.FC<{ hasText?: boolean }>
      }[]
    | undefined

type TableActionsPropsT = {
    selectedRowIndex: number | undefined
    isFocus: boolean
    focusRect: any
    rowRect: any
    actions: any
    mountNode?: HTMLElement
}

function ActionMenu({
    options: renderOptions = [],
    optionFilter = () => true,
    isOpen = false,
    children,
    Content,
    ...rest
}: {
    options: { type: string | number; label: any }[]
    optionFilter?: (option: any) => boolean
    isOpen?: boolean
    onItemSelect?: (value: string) => void
    placement?: any
    children?: React.ReactNode
    Content?: React.FC<any>
    mountNode?: HTMLElement
}) {
    return (
        <PopoverContainer
            {...rest}
            options={renderOptions.filter(optionFilter)}
            isOpen={isOpen}
            Content={Content ?? SingleSelectMenu}
            onItemSelect={({ item }) => rest.onItemSelect?.(item.type)}
            contentOverrides={listOverrides}
        >
            {children}
        </PopoverContainer>
    )
}
const QUICK_PADDING = 16
const QUICK_RIGHT_OFFSET = 20
const QUICK_ACTION_WIDTH = 32
const QUICK_TOP_OFFSET = 5

function TableActions({ selectedRowIndex = -1, isFocus, focusRect, rowRect, actions, mountNode }: TableActionsPropsT) {
    const $focusActions = useCreation(() => {
        const validActions = actions?.filter((action) => action.access)

        const noneQuickActions = validActions?.filter((action) => !action.quickAccess) ?? []
        const quickActions = validActions?.filter((action) => action.quickAccess) ?? []
        const renderer = (action, index) => {
            return {
                type: index,
                label: <action.component key={index} hasText />,
            }
        }

        return [
            ...quickActions.map(renderer),
            ...(noneQuickActions.length > 0
                ? [
                      {
                          type: -1,
                          label: (
                              <div className='py-4px w-full bg-[#fff]'>
                                  <p className=' h-1px bg-[#EEF1F6]' />
                              </div>
                          ),
                      },
                  ]
                : []),
            ...noneQuickActions.map(renderer),
        ]
    }, [isFocus])

    const [$quickActions, $quickMoreActions] = useCreation(() => {
        return [
            actions
                ?.filter((action) => action.access && action.quickAccess)
                .map((action, index) => {
                    return <action.component key={index} />
                }),
            actions
                ?.filter((action) => action.access && !action.quickAccess)
                .map((action, index) => {
                    return {
                        type: index,
                        label: <action.component key={index} hasText />,
                    }
                }),
        ]
    }, [actions, isFocus])

    const quickActionCount = $quickActions?.length + ($quickMoreActions?.length > 0 ? 1 : 0)

    const $focusRect = React.useMemo(() => {
        const len = $focusActions?.length ?? 0
        const offset = {
            left: -1 * (len + 1) * QUICK_ACTION_WIDTH - QUICK_PADDING - QUICK_RIGHT_OFFSET,
            top: QUICK_TOP_OFFSET,
        }

        const { left = 0, top = 0 } = focusRect ?? {}

        return {
            left,
            top: top + offset.top,
        }
    }, [$focusActions, focusRect])

    const $rowRect = React.useMemo(() => {
        const offset = {
            left: -1 * quickActionCount * QUICK_ACTION_WIDTH - QUICK_PADDING - QUICK_RIGHT_OFFSET,
            top: QUICK_TOP_OFFSET,
        }

        const { left = 0, top = 0 } = rowRect ?? {}

        return {
            left: left + offset.left,
            top: top + offset.top,
        }
    }, [quickActionCount, rowRect])

    if (!actions) return null

    return (
        <>
            {$focusActions && (
                <ActionMenu
                    isOpen={selectedRowIndex >= 0 && isFocus}
                    options={$focusActions}
                    placement='right'
                    mountNode={mountNode}
                    // @ts-ignore
                    overrides={getPopoverOverrides($focusRect)}
                />
            )}
            <ActionMenu
                isOpen={selectedRowIndex >= 0 && !isFocus}
                placement='right'
                mountNode={mountNode}
                // @ts-ignore
                overrides={getPopoverOverrides($rowRect)}
                Content={() => {
                    return (
                        <div className='f-c-c px-8px'>
                            {$quickActions}
                            {$quickMoreActions && $quickMoreActions.length > 0 && (
                                <StatefulPopover
                                    triggerType='hover'
                                    placement='bottom'
                                    overrides={{
                                        Body: {
                                            props: {
                                                className: 'filter-popover',
                                            },
                                            style: {
                                                marginTop: '0px',
                                                boxShadow: '#CFD7E6 0px 2px 2px',
                                                backgroundColor: '#F5F8FF',
                                            },
                                        },
                                    }}
                                    content={() => {
                                        return (
                                            <SingleSelectMenu overrides={listOverrides} options={$quickMoreActions} />
                                        )
                                    }}
                                >
                                    <ExtendButton icon='more' styleas={['highlight']} />
                                </StatefulPopover>
                            )}
                        </div>
                    )
                }}
            />
        </>
    )
}

export type { TableActionsPropsT, TableActionsT }

export default TableActions
