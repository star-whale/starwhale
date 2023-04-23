import React, { useMemo, useState, useEffect } from 'react'

export function useIsInViewport(ref: React.MutableRefObject<HTMLElement>) {
    const [isIntersecting, setIsIntersecting] = useState(false)

    const observer = useMemo(
        () =>
            new IntersectionObserver(([entry]) => {
                setIsIntersecting(entry.isIntersecting)
                console.log(entry, ref.current)
            }),
        []
    )

    useEffect(() => {
        observer.observe(ref.current)

        return () => {
            observer.disconnect()
        }
    }, [ref, observer])

    return isIntersecting
}
