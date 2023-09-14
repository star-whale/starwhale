import React from 'react'
import { StatefulPopover } from 'baseui/popover'
import Button from '@starwhale/ui/Button'
import { DatastoreMixedTypeSearch } from '@starwhale/ui/Search/Search'
import IconFont from '@starwhale/ui/IconFont'
import { QueryT } from '@starwhale/ui/base/data-table/types'
import { ColumnSchemaDesc } from '@starwhale/core'
import { sortColumn } from '@starwhale/ui/GridDatastoreTable'
import _ from 'lodash'

type PropsT = {
    columnTypes?: ColumnSchemaDesc[]
    value: QueryT[]
    onChange: (args: QueryT[]) => void
}

export type ExtraPropsT = {
    width?: number
    isAction?: boolean
    mountNode?: HTMLElement
    isOpen?: boolean
    setIsOpen?: (isOpen: boolean) => void
}

function ConfigQuery(props: PropsT) {
    const fields = React.useMemo(() => {
        return (props.columnTypes ?? [])
            .map((column) => {
                return {
                    name: column.name,
                    type: column.type,
                }
            })
            .sort(sortColumn as any)
    }, [props.columnTypes])

    return <DatastoreMixedTypeSearch fields={fields as any} value={props.value} onChange={props.onChange} />
}

function ConfigQueryInline(props: ExtraPropsT & PropsT) {
    const { isOpen, setIsOpen } = props

    return (
        <>
            {isOpen && (
                <StatefulPopover
                    focusLock
                    initialState={{
                        isOpen: true,
                    }}
                    mountNode={props.mountNode}
                    overrides={{
                        Body: {
                            style: {
                                width: _.isNumber(props.width) ? `${props.width}px` : '100%',
                                /* tricky here: to make sure popover to align with rows, if cell
                                 changes ,this should be reset */
                                marginLeft: '-12px',
                                marginBottom: '-5px',
                                zIndex: '3 !important',
                            },
                        },
                        Inner: {
                            style: {
                                backgroundColor: '#FFF',
                            },
                        },
                    }}
                    dismissOnEsc={false}
                    // dismissOnClickOutside={false}
                    placement='topLeft'
                    content={() => <ConfigQuery {...props} />}
                    onClose={() => {
                        setIsOpen?.(false)
                    }}
                >
                    <span />
                </StatefulPopover>
            )}

            {props.isAction && (
                <Button onClick={() => setIsOpen?.(!isOpen)} as='link'>
                    <div
                        style={{
                            padding: '5px',
                            backgroundColor: isOpen ? '#DBE5F9' : 'transparent',
                            borderRadius: '4px',
                        }}
                    >
                        <IconFont type='filter' />
                    </div>
                </Button>
            )}
        </>
    )
}

function StatefulConfigQueryInline(props) {
    const [isOpen, setIsOpen] = React.useState(false)

    return <ConfigQueryInline {...props} isOpen={isOpen} setIsOpen={setIsOpen} />
}

export { ConfigQueryInline, StatefulConfigQueryInline, ConfigQuery }
export default ConfigQuery
