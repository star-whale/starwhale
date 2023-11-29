import React, { useState } from 'react'
import { Resizable } from 'react-resizable'
import 'react-resizable/css/styles.css'

const RECT = { width: 400, height: 330 }

function CasecadeResizer({ children, minConstraints = [400, 330], maxConstraints = [1000, 1000] }) {
    const [rect, setRect] = useState(RECT)
    const onResize = (e: any, { size }: any) => {
        setRect({
            width: size.width,
            height: size.height,
        })
    }

    const memoChildren = React.useMemo(() => {
        return React.Children.map(children as any, (child: React.ReactElement) => {
            if (!child) return null
            return (
                <Resizable
                    width={rect.width}
                    height={rect.height}
                    axis='both'
                    onResize={onResize}
                    maxConstraints={maxConstraints}
                    minConstraints={minConstraints}
                >
                    <div
                        className='box inline-block flex-shrink-0 flex-grow-0'
                        style={{
                            ...rect,
                        }}
                    >
                        {child}
                    </div>
                </Resizable>
            )
        })
    }, [children, rect, minConstraints, maxConstraints])

    return memoChildren
}

export { CasecadeResizer }

export default CasecadeResizer
