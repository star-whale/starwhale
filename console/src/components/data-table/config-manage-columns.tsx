import React, { useMemo, useCallback, useRef, useEffect, useImperativeHandle } from 'react'
import { SHAPE, SIZE, KIND } from 'baseui/button'
import { Search } from 'baseui/icon'
import { useStyletron } from 'baseui'
import { useHover } from 'react-use'
import { Drawer } from 'baseui/drawer'
import { Checkbox } from 'baseui/checkbox'
import { LabelSmall } from 'baseui/typography'
import Button from '@/components/Button'
import Input from '@/components/Input'
import useSelection from '@/hooks/useSelection'
import { AiOutlinePushpin } from 'react-icons/ai'
import { RiDeleteBin6Line } from 'react-icons/ri'
import { useDrawer } from '@/hooks/useDrawer'
import IconFont from '@/components/IconFont'
import { DnDContainer } from '../DnD/DnDContainer'
import { matchesQuery } from './text-search'
import type { ColumnT, ConfigT } from './types'
// import { LocaleContext } from './locales'

type PropsT = {
    isInline?: boolean
    view: ConfigT
    columns: ColumnT[]
    onApply?: (columnSortedIds: T[], columnVisibleIds: T[], pinnedIds: T[]) => void
    onSave?: (view: ConfigT) => void
    onSaveAs?: (view: ConfigT) => void
}

type T = string
const ConfigManageColumns = React.forwardRef<{ getConfig: () => any }, PropsT>((props, configRef) => {
    const [css, theme] = useStyletron()
    // const locale = React.useContext(LocaleContext)
    const [isOpen, setIsOpen] = React.useState(false)
    const [query, setQuery] = React.useState('')
    const { expandedWidth, expanded, setExpanded } = useDrawer()

    useEffect(() => {
        if (props.isInline) {
            return
        }
        if (isOpen && !expanded) {
            setExpanded(true)
        } else if (!isOpen && expanded) {
            setExpanded(false)
        }
    }, [props.isInline, isOpen, expanded, setExpanded, expandedWidth])

    const ref = useRef(null)
    const { columns } = props
    const columnAllIds = useMemo(() => {
        return columns.map((v) => v.key as string)
    }, [columns])
    const matchedColumns = React.useMemo(() => {
        return columns.filter((column) => matchesQuery(column.title, query)) ?? []
    }, [columns, query])
    const columnMatchedIds = useMemo(() => {
        return matchedColumns.map((v) => v.key) ?? []
    }, [matchedColumns])

    const {
        selectedIds,
        sortedIds,
        pinnedIds,
        handleSelectMany,
        handleSelectNone,
        handleSelectOne,
        handleOrderChange,
        handlePinOne,
        handleEmpty,
    } = useSelection<T>({
        initialSelectedIds: props.view?.selectedIds ?? columnAllIds,
        initialPinnedIds: props.view?.pinnedIds ?? [],
        initialSortedIds: props.view?.sortedIds ?? [],
    })

    const dndData = useMemo(() => {
        const DnDCell = ({ column, pined }: { column: ColumnT; pined: boolean }) => {
            const [hoverable] = useHover((hoverd) => {
                if (!column) return <></>

                return (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            paddingLeft: '10px',
                            paddingRight: '9px',
                            height: '32px',
                            cursor: 'pointer',
                            willChange: 'transform',
                            flexWrap: 'nowrap',
                            justifyContent: 'space-between',
                            background: hoverd ? '#F0F4FF' : '#FFFFFF',
                        }}
                        title={column.title}
                    >
                        <LabelSmall $style={{ flex: 1, overflow: 'hidden', lineHeight: '1.2' }} className='line-clamp'>
                            {column.title}
                        </LabelSmall>
                        <div>
                            {(pined || hoverd) && (
                                <Button
                                    overrides={{
                                        BaseButton: {
                                            style: {
                                                'paddingLeft': '7px',
                                                'paddingRight': '7px',
                                                'color': pined ? 'rgba(2,16,43,0.80)' : 'rgba(2,16,43,0.20)',
                                                ':hover': {
                                                    background: 'transparent',
                                                    color: pined ? '#02102B' : 'rgba(2,16,43,0.50)',
                                                },
                                            },
                                        },
                                    }}
                                    as='transparent'
                                    onClick={() => handlePinOne(column.key as string)}
                                >
                                    <AiOutlinePushpin size={16} />
                                </Button>
                            )}
                            <Button
                                overrides={{
                                    BaseButton: {
                                        style: {
                                            paddingLeft: '7px',
                                            paddingRight: '7px',
                                            color: 'rgba(2,16,43,0.40)',
                                        },
                                    },
                                }}
                                as='transparent'
                                onClick={() => handleSelectOne(column.key as string)}
                            >
                                <RiDeleteBin6Line size={16} />
                            </Button>
                        </div>
                    </div>
                )
            })

            return hoverable
        }

        return selectedIds.map((id) => {
            const column = columns.find((v) => v.key === id)

            if (!column) return { id, text: <></> }
            return {
                id: column?.key as string,
                // @ts-ignore
                text: <DnDCell column={column} pined={pinnedIds.includes(id)} />,
            }
        })
    }, [selectedIds, pinnedIds, columns, handlePinOne, handleSelectOne])

    const handleSave = useCallback(() => {
        props.onSave?.({
            ...props.view,
            selectedIds,
            sortedIds,
            pinnedIds,
        })
    }, [props, selectedIds, sortedIds, pinnedIds])
    const handleSaveAs = useCallback(() => {
        props.onSaveAs?.({
            ...props.view,
            selectedIds,
            sortedIds,
            pinnedIds,
        })
    }, [props, selectedIds, sortedIds, pinnedIds])
    const handleApply = useCallback(() => {
        props.onApply?.(selectedIds, pinnedIds, sortedIds)
    }, [props, selectedIds, sortedIds, pinnedIds])

    useImperativeHandle(
        configRef,
        () => ({
            getConfig: () => {
                return {
                    selectedIds,
                    sortedIds,
                    pinnedIds,
                }
            },
        }),
        [selectedIds, sortedIds, pinnedIds]
    )

    const Wrapper = React.useCallback(
        // eslint-disable-next-line react/no-unused-prop-types
        ({ children }: { children: React.ReactNode }) => {
            return props.isInline ? (
                <div>{children}</div>
            ) : (
                ref.current && (
                    <Drawer
                        size='520px'
                        isOpen={isOpen}
                        autoFocus
                        // showBackdrop={false}
                        onClose={() => setIsOpen(false)}
                        // mountNode={document.body}
                        overrides={{
                            Root: {
                                style: {
                                    zIndex: '102',
                                    margin: 0,
                                },
                            },
                            DrawerContainer: {
                                style: {
                                    borderRadius: '0',
                                    boxSizing: 'border-box',
                                    padding: '0px 0 10px',
                                    boxShadow: '0 4px 14px 0 rgba(0, 0, 0, 0.3)',
                                    margin: 0,
                                },
                            },
                            DrawerBody: {
                                style: {
                                    marginLeft: 0,
                                    marginRight: 0,
                                    marginTop: 0,
                                    marginBottom: 0,
                                },
                            },
                        }}
                    >
                        {children}
                    </Drawer>
                )
            )
        },
        [props.isInline, isOpen]
    )

    return (
        <div ref={ref}>
            {!props.isInline && (
                <Button
                    onClick={() => setIsOpen(!isOpen)}
                    shape={SHAPE.pill}
                    size={SIZE.compact}
                    as='link'
                    startEnhancer={() => (
                        <IconFont
                            type='setting'
                            style={{
                                marginRight: '-5px',
                                marginTop: 'px',
                            }}
                        />
                    )}
                    overrides={{
                        BaseButton: {
                            style: {
                                height: '32px',
                                marginLeft: theme.sizing.scale500,
                            },
                        },
                    }}
                >
                    Manage Columns
                </Button>
            )}
            {ref.current && (
                <Wrapper>
                    <div
                        className={css(
                            props.isInline
                                ? {
                                      display: 'flex',
                                      flexDirection: 'column',
                                      height: '56px',
                                      lineHeight: '56px',
                                      borderTop: '1px solid #EEF1F6',
                                      paddingLeft: 0,
                                      marginTop: '20px',
                                      fontWeight: 'bold',
                                  }
                                : {
                                      display: 'flex',
                                      flexDirection: 'column',
                                      height: '56px',
                                      lineHeight: '56px',
                                      borderBottom: '1px solid #EEF1F6',
                                      paddingLeft: '20px',
                                      marginBottom: '20px',
                                  }
                        )}
                    >
                        Manage Columns
                    </div>
                    <div
                        className={css({
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '20px',
                            height: 'calc(100% - 76px)',
                            paddingLeft: props.isInline ? 0 : '20px',
                            paddingRight: props.isInline ? 0 : '20px',
                        })}
                    >
                        <div
                            className={css({
                                width: '280px',
                            })}
                        >
                            <Input
                                overrides={{
                                    Before: function Before() {
                                        return (
                                            <div
                                                className={css({
                                                    alignItems: 'center',
                                                    display: 'flex',
                                                    paddingLeft: theme.sizing.scale500,
                                                })}
                                            >
                                                <Search size='18px' />
                                            </div>
                                        )
                                    },
                                }}
                                value={query}
                                // @ts-ignore
                                onChange={(event) => setQuery(event.target.value)}
                            />
                        </div>
                        <div
                            className={css({
                                flex: 1,
                                border: '1px solid #CFD7E6',
                                borderRadius: '4px',
                                display: 'flex',
                            })}
                        >
                            {/* All columns edit */}
                            <div
                                className={css({
                                    borderRight: '1px solid #CFD7E6',
                                    flex: '1 0 50%',
                                })}
                            >
                                <div
                                    className={css({
                                        display: 'flex',
                                        height: '42px',
                                        borderBottom: '1px solid #EEF1F6',
                                        marginBottom: '8px',
                                        fontSize: '14px',
                                        marginLeft: '10px',
                                        marginRight: '9px',
                                        alignItems: 'center',
                                        gap: '9px',
                                    })}
                                >
                                    <Checkbox
                                        checked={selectedIds.length === columns.length}
                                        onChange={(e) =>
                                            // @ts-ignore
                                            e.target?.checked ? handleSelectMany(columnAllIds) : handleSelectNone()
                                        }
                                    />
                                    <LabelSmall>All columns</LabelSmall>
                                    <span
                                        style={{
                                            marginLeft: '-5px',
                                            color: 'rgba(2,16,43,0.40)',
                                        }}
                                    >
                                        ({selectedIds.length}/{columns.length})
                                    </span>
                                </div>
                                <div
                                    className={css({
                                        display: 'flex',
                                        flexDirection: 'column',
                                    })}
                                >
                                    {columnAllIds.map((id) => {
                                        if (!columnMatchedIds.includes(id)) {
                                            return null
                                        }
                                        const column = columns.find((v) => v.key === id)
                                        if (!column) return null

                                        return (
                                            <div
                                                key={id}
                                                style={{
                                                    paddingLeft: '10px',
                                                    paddingRight: '9px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: '9px',
                                                    height: '32px',
                                                    willChange: 'transform',
                                                    flexWrap: 'nowrap',
                                                    justifyContent: 'space-between',
                                                }}
                                                title={column.title}
                                            >
                                                <Checkbox
                                                    checked={selectedIds?.includes(id)}
                                                    onChange={() => handleSelectOne(id)}
                                                />
                                                <LabelSmall
                                                    $style={{ flex: 1, overflow: 'hidden', lineHeight: '1.1' }}
                                                    className='line-clamp'
                                                >
                                                    {column.title}
                                                </LabelSmall>
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                            {/* Visible columns edit */}
                            <div className={css({ flex: '1 0 50%' })}>
                                <div
                                    className={css({
                                        display: 'flex',
                                        height: '42px',
                                        borderBottom: '1px solid #EEF1F6',
                                        marginBottom: '8px',
                                        marginLeft: '10px',
                                        marginRight: '9px',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                    })}
                                >
                                    <p>
                                        Visible columns
                                        <span
                                            style={{
                                                marginLeft: '5px',
                                                color: 'rgba(2,16,43,0.40)',
                                            }}
                                        >
                                            ({selectedIds.length})
                                        </span>
                                    </p>
                                    <Button as='link' onClick={handleEmpty}>
                                        Clear
                                    </Button>
                                </div>
                                {dndData.length === 0 && (
                                    <div className='flex-column-center'>
                                        <div
                                            style={{
                                                background: '#EEF1F6',
                                                borderRadius: '1px',
                                                width: '64px',
                                                height: '64px',
                                                marginBottom: '20px',
                                                marginTop: '68px',
                                            }}
                                        />
                                        <p
                                            style={{
                                                color: 'rgba(2,16,43,0.40)',
                                                maxWidth: '189px',
                                                textAlign: 'center',
                                            }}
                                        >
                                            Please select a column in the left side
                                        </p>
                                    </div>
                                )}
                                {dndData.length > 0 && (
                                    <DnDContainer onOrderChange={handleOrderChange} data={dndData} />
                                )}
                            </div>
                        </div>
                        {!props.isInline && (
                            <div
                                className={css({
                                    display: 'flex',
                                    justifyContent: 'start',
                                    gap: '20px',
                                })}
                            >
                                <Button onClick={handleSaveAs} kind={KIND.secondary} size={SIZE.mini}>
                                    Save AS
                                </Button>
                                <Button onClick={handleSave} kind={KIND.secondary}>
                                    Save
                                </Button>
                                <Button
                                    overrides={{
                                        BaseButton: {
                                            style: {
                                                marginLeft: 'auto',
                                            },
                                        },
                                    }}
                                    size={SIZE.mini}
                                    onClick={handleApply}
                                >
                                    Apply
                                </Button>
                            </div>
                        )}
                    </div>
                </Wrapper>
            )}
        </div>
    )
})

ConfigManageColumns.defaultProps = {
    isInline: false,
    onApply: () => {},
    onSave: () => {},
    onSaveAs: () => {},
}
export default ConfigManageColumns
