import { Popover } from 'baseui/popover'
import { PLACEMENT } from 'baseui/toast'
import React, { useEffect, useState } from 'react'

function SelectorPopover({
    isOpen: $isOpen,
    children,
    content,
    rows,
}: {
    rows: number
    isOpen: boolean
    children?: React.ReactNode
    content: React.ReactNode | (() => React.ReactNode)
}) {
    const [isOpen, setIsOpen] = useState(false)
    const ref = React.useRef<HTMLElement>(null)

    useEffect(() => {
        setIsOpen($isOpen)
    }, [$isOpen])

    const handleClose = () => ref.current && setIsOpen(false)

    return (
        <Popover
            placement={PLACEMENT.bottomRight}
            showArrow
            autoFocus={false}
            isOpen={isOpen}
            innerRef={ref}
            onEsc={handleClose}
            overrides={{
                Body: {
                    style: {
                        marginTop: `${(rows + 1) * 22 + 10}px`,
                        bottom: 0,
                    },
                },
                Inner: {
                    style: {
                        padding: '12px 12px',
                        backgroundColor: '#FFF',
                        minHeight: '200px',
                        minWidth: '410px',
                        // maxHeight: '600px',
                        overflow: 'auto',
                    },
                },
            }}
            content={() => <div className='popover'>{typeof content === 'function' ? content() : content}</div>}
        >
            <p>{children}</p>
        </Popover>
    )
}
export default SelectorPopover
