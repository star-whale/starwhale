import React from 'react'
import AutosizeInput from '../../base/select/autosize-input'

function FieldInputWrapper({ focused, width, ...props }) {
    return (
        <div
            className='autosize-input inline-block relative flex-1 h-full max-w-full'
            style={{
                minWidth: focused ? '50px' : 0,
                flexBasis: focused ? '100px' : 0,
                width: focused ? `${width}px` : 0,
            }}
        >
            {props.children}
        </div>
    )
}

function FieldInput({
    focused,
    inputRef,
    value,
    onChange,
    overrides,
    width = 160,
}: {
    focused: boolean
    inputRef: React.RefObject<HTMLInputElement>
    value: string
    onChange: (e: any) => void
    overrides?: any
    width?: number
}) {
    return (
        <FieldInputWrapper focused={focused} width={width}>
            {/* @ts-ignore */}
            <AutosizeInput
                inputRef={inputRef as any}
                value={value}
                onChange={onChange}
                overrides={overrides}
                $style={{ width: '100%', height: '100%' }}
            />
        </FieldInputWrapper>
    )
}

export { FieldInputWrapper }

export default FieldInput
