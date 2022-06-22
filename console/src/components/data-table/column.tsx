/* eslint-disable */

/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import * as React from 'react'

import { Checkbox } from 'baseui/checkbox'
import { useStyletron } from 'baseui'

import type { ColumnT } from './types.js'
import _ from 'lodash'

function Column<ValueT, FilterParamsT>(options: ColumnT<ValueT, FilterParamsT>): ColumnT<ValueT, FilterParamsT> {
    return {
        kind: options.kind,
        buildFilter: options.buildFilter || (() => () => true),
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
                    className={css({
                        ...theme.typography.font100,
                        boxSizing: 'border-box',
                        color: theme.colors.contentPrimary,
                        // @ts-ignore
                        display: props.isMeasured ? 'inline-block' : null,
                        // height: '100%',
                        // paddingTop: theme.sizing.scale300,
                        // paddingLeft: theme.sizing.scale500,
                        // paddingBottom: theme.sizing.scale300,
                        // paddingRight: theme.sizing.scale500,
                        // @ts-ignore
                        width: props.isMeasured ? null : '100%',
                        padding: '0',
                    })}
                >
                    <div
                        data-type='column'
                        className={css({
                            alignItems: cellBlockAlign,
                            display: 'flex',
                            height: '100%',
                        })}
                    >
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
                                        Checkmark: { style: { marginTop: null, marginBottom: null } },
                                    }}
                                />
                            </span>
                        )}
                        {/* @ts-ignore */}
                        <ProvidedCell {...props} />
                    </div>
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
    }
}

export default Column
