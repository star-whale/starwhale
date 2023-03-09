import { Popover } from 'baseui/popover'
import { PLACEMENT } from 'baseui/toast'
import React, { useEffect, useState } from 'react'
import Tree from './Tree'
import { TreePropsT } from './types'

function SelectorPopover({
    isOpen: $isOpen,
    children,
    content,
    ...rest
}: TreePropsT & { isOpen: boolean; children: React.ReactNode }) {
    const [isOpen, setIsOpen] = useState(false)
    const ref = React.useRef<HTMLElement>(null)

    useEffect(() => {
        setIsOpen($isOpen)
    }, [$isOpen])

    const handleClose = () => ref.current && setIsOpen(false)

    console.log('TreePopover', isOpen)

    return (
        <Popover
            placement={PLACEMENT.bottomLeft}
            autoFocus={false}
            isOpen={isOpen}
            innerRef={ref}
            onEsc={handleClose}
            overrides={{
                Body: {
                    style: {
                        marginTop: '32px',
                    },
                },
                Inner: {
                    style: {
                        padding: '8px 12px',
                        backgroundColor: '#FFF',
                        minHeight: '200px',
                    },
                },
            }}
            content={() => <div className='popover'>{content()}</div>}
        >
            <p>{children}</p>
        </Popover>
    )
}
export default SelectorPopover
