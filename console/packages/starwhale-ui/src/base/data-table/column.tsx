/* eslint-disable */

import * as React from 'react'

import { useStyletron } from 'baseui'
import type { ColumnT } from './types.js'
import _ from 'lodash'
import cn from 'classnames'
import Checkbox from '../../Checkbox'
import Button from '../../Button'
import IconFont from '../../IconFont'
import { themedUseStyletron } from '../../theme/styletron'

const MIN_WIDTH = 100
const MAX_WIDTH = 500

function Column<ValueT, FilterParamsT>(options: ColumnT<ValueT, FilterParamsT>): ColumnT<ValueT, FilterParamsT> {
    const RenderCell = React.forwardRef<
        HTMLDivElement,
        {
            isQueryInline?: boolean
            onSelect?: () => void
            isSelected?: boolean
        }
    >((props, ref) => {
        const [css, theme] = themedUseStyletron()
        const ProvidedCell = options.renderCell

        let cellBlockAlign = 'flex-start'
        if (options.cellBlockAlign === 'center') {
            cellBlockAlign = 'center'
        } else if (options.cellBlockAlign === 'end') {
            cellBlockAlign = 'flex-end'
        }

        return (
            <div
                // @ts-ignore
                ref={ref}
                className={cn(
                    'column-cell',
                    css({
                        ...theme.typography.font200,
                        boxSizing: 'border-box',
                        color: theme.colors.contentPrimary,
                        padding: '0',
                        height: '100%',
                        alignItems: 'center',
                        display: 'flex',
                        width: '100%',
                        justifyContent: cellBlockAlign,
                    })
                )}
            >
                {Boolean(props.isQueryInline) && (
                    <p
                        className={css({
                            flexBasis: '32px',
                            flexShrink: 0,
                        })}
                    />
                )}
                {Boolean(props.onSelect) && (
                    <span className={css({ paddingRight: theme.sizing.scale300 })}>
                        {/* @ts-ignore */}
                        <Checkbox
                            //@ts-ignore
                            onChange={props.onSelect}
                            //@ts-ignore
                            checked={props.isSelected}
                            overrides={{
                                Checkmark: { style: { marginTop: 0, marginBottom: 0 } },
                            }}
                        />
                    </span>
                )}
                {/* @ts-ignore */}
                <ProvidedCell {...props} />
            </div>
        )
    })

    RenderCell.displayName = 'Cell'

    return {
        kind: options.kind,
        buildFilter: options.buildFilter || ((params) => (data) => true),
        textQueryFilter: options.textQueryFilter,
        fillWidth: options.fillWidth === undefined ? true : options.fillWidth,
        filterable: Boolean(options.filterable) && Boolean(options.renderFilter) && Boolean(options.buildFilter),
        mapDataToValue: options.mapDataToValue,
        maxWidth: options.maxWidth ?? MAX_WIDTH,
        minWidth: options.minWidth ?? MIN_WIDTH,
        // @ts-ignore
        renderCell: RenderCell,
        renderFilter: options.renderFilter || (() => null),
        sortable: Boolean(options.sortable) && Boolean(options.sortFn),
        sortFn: options.sortFn || (() => 0),
        title: options.title,
        onAsyncChange: options?.onAsyncChange,
        key: options.key ?? options.title.toLocaleLowerCase().replace(' ', ''),
        pin: options.pin,
        filterType: options.filterType,
        columnType: options.columnType,
    }
}

export default Column
