import React from 'react'
import type { FC } from 'react'
import { useRef } from 'react'
import { useStyletron } from 'baseui'

export interface CardProps {
    text: React.ReactElement
    preview?: boolean
}

export const DnDCardDragPreview: FC<CardProps> = ({ text, preview }) => {
    const [css] = useStyletron()
    const ref = useRef<HTMLDivElement>(null)
    const backgroundColor = '#F0F4FF'

    return (
        <div
            ref={ref}
            role=''
            className={css({
                height: '32px',
                lineheight: '16px',
                display: 'flex',
                alignItems: 'center',
                cursor: 'pointer',
                willchange: 'transform',
                flexWrap: 'nowrap',
                justifyContent: 'space-between',
                backgroundColor,
            })}
        >
            {text}
        </div>
    )
}
