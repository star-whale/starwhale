import React, { useEffect, useRef, useState } from 'react'

export default function useScrollToBottom() {
    // for auto-scroll
    const scrollRef = useRef<HTMLDivElement>(null)
    const [autoScroll, setAutoScroll] = useState(true)

    function scrollDomToBottom() {
        const dom = scrollRef.current
        if (dom) {
            requestAnimationFrame(() => {
                setAutoScroll(true)
                dom.scrollTo(0, dom.scrollHeight)
            })
        }
    }

    // auto scroll
    useEffect(() => {
        if (autoScroll) {
            scrollDomToBottom()
        }
    })

    return {
        scrollRef,
        autoScroll,
        setAutoScroll,
        scrollDomToBottom,
    }
}
