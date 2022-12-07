/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import * as React from 'react'
import { getOverrides } from 'baseui/helpers/overrides'
import { Tag, VARIANT as TAG_VARIANT } from 'baseui/tag'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function MultiValue(props: any) {
    const { overrides = {}, removeValue, ...restProps } = props
    // todo(v11): remove the MultiValue override in favor of Tag
    const [MultiValue, tagProps] = getOverrides(overrides.Tag || overrides.MultiValue, Tag)
    return (
        // @ts-ignore TS2786 error with web-eats-v2, can remove once React 18 migration complete
        <MultiValue
            variant={TAG_VARIANT.solid}
            overrides={{
                Root: {
                    // @ts-ignore
                    style: ({ $theme: { sizing } }) => ({
                        marginRight: sizing.scale0,
                        marginBottom: sizing.scale0,
                        marginLeft: sizing.scale0,
                        marginTop: sizing.scale0,
                    }),
                },
            }}
            onActionClick={removeValue}
            {...restProps}
            {...tagProps}
        >
            {props.children}
        </MultiValue>
    )
}
