import React from 'react'
import { StatefulPopover } from 'baseui/popover'
import Button from '@starwhale/ui/Button'
import { DatastoreMixedTypeSearch } from '@starwhale/ui/Search/Search'
import IconFont from '@starwhale/ui/IconFont'
import { QueryT } from '@starwhale/ui/base/data-table/types'
import { ColumnSchemaDesc } from '@starwhale/core'

type PropsT = {
    columnTypes?: ColumnSchemaDesc[]
    value: QueryT[]
    onChange: (args: QueryT[]) => void
}

function ConfigQuery(props: PropsT) {
    return <DatastoreMixedTypeSearch fields={props.columnTypes as any} value={props.value} onChange={props.onChange} />
}

function ConfigQueryInline(props: PropsT & { width: number }) {
    const [isOpen, setIsOpen] = React.useState(false)
    return (
        <>
            {isOpen && (
                <StatefulPopover
                    focusLock
                    initialState={{
                        isOpen: true,
                    }}
                    overrides={{
                        Body: {
                            style: {
                                width: `${props.width}px`,
                                /* tricky here: to make sure popover to align with rows, if cell
                                 changes ,this should be reset */
                                marginLeft: '-12px',
                                zIndex: '2 !important',
                            },
                        },
                        Inner: {
                            style: {
                                backgroundColor: '#FFF',
                            },
                        },
                    }}
                    dismissOnEsc={false}
                    dismissOnClickOutside={false}
                    placement='topLeft'
                    content={() => <ConfigQuery {...props} />}
                >
                    <span />
                </StatefulPopover>
            )}

            <Button onClick={() => setIsOpen(!isOpen)} as='link'>
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
        </>
    )
}

export { ConfigQueryInline, ConfigQuery }
export default ConfigQuery
