/* eslint-disable */

import * as React from 'react'

import { useStyletron } from 'baseui'
import type { ColumnT } from './types.js'
import _ from 'lodash'
import cn from 'classnames'
import Checkbox from '@/components/Checkbox'

function Column<ValueT, FilterParamsT>(options: ColumnT<ValueT, FilterParamsT>): ColumnT<ValueT, FilterParamsT> {
    return {
        kind: options.kind,
        buildFilter: options.buildFilter || ((params) => (data) => true),
        textQueryFilter: options.textQueryFilter,
        fillWidth: options.fillWidth === undefined ? true : options.fillWidth,
        filterable: Boolean(options.filterable) && Boolean(options.renderFilter) && Boolean(options.buildFilter),
        mapDataToValue: options.mapDataToValue,
        maxWidth: options.maxWidth,
        minWidth: options.minWidth,
        // @ts-ignore
        renderCell: React.forwardRef((props, ref) => {
            const [css, theme] = useStyletron()
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
                            // @ts-ignore
                            // display: props.isMeasured ? 'inline-block' : undefined,
                            // @ts-ignore
                            // width: props.isMeasured ? undefined : '100%',
                            padding: '0',
                            height: '100%',
                            alignItems: 'center',
                            display: 'flex',
                            width: '100%',
                        })
                    )}
                >
                    {/* <div
                        data-type='column-cell-1'
                        className={css({
                            flex: 1,
                            alignItems: 'center',
                            display: 'flex',
                            height: '100%',
                        })}
                    > */}
                    {/* @ts-ignore */}
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
                    {/* </div> */}
                </div>
            )
        }),
        renderFilter: options.renderFilter || (() => null),
        sortable: Boolean(options.sortable) && Boolean(options.sortFn),
        sortFn: options.sortFn || (() => 0),
        title: options.title,
        onAsyncChange: options?.onAsyncChange,
        key: options.key ?? options.title.toLocaleLowerCase().replace(' ', ''),
        pin: options.pin,
        filterType: options.filterType,
    }
}

export default Column
