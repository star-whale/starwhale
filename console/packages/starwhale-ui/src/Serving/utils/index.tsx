export function isMacOS(): boolean {
    if (typeof window !== 'undefined') {
        const userAgent = window.navigator.userAgent.toLocaleLowerCase()
        const macintosh = /iphone|ipad|ipod|macintosh/.test(userAgent)
        return !!macintosh
    }
    return false
}

function getDomContentWidth(dom: HTMLElement) {
    const style = window.getComputedStyle(dom)
    const paddingWidth = parseFloat(style.paddingLeft) + parseFloat(style.paddingRight)
    const width = dom.clientWidth - paddingWidth
    return width
}

function getOrCreateMeasureDom(id: string, init?: (dom: HTMLElement) => void) {
    let dom = document.getElementById(id)

    if (!dom) {
        dom = document.createElement('span')
        dom.style.position = 'absolute'
        dom.style.wordBreak = 'break-word'
        dom.style.fontSize = '14px'
        dom.style.transform = 'translateY(-200vh)'
        dom.style.pointerEvents = 'none'
        dom.style.opacity = '0'
        dom.id = id
        document.body.appendChild(dom)
        init?.(dom)
    }

    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    return dom!
}

export function autoGrowTextArea(dom: HTMLTextAreaElement) {
    const measureDom = getOrCreateMeasureDom('__measure')
    const singleLineDom = getOrCreateMeasureDom('__single_measure', (_dom) => {
        // eslint-disable-next-line no-param-reassign
        _dom.innerText = 'TEXT_FOR_MEASURE'
    })

    const width = getDomContentWidth(dom)
    measureDom.style.width = `${width}px`
    measureDom.innerText = dom.value !== '' ? dom.value : '1'
    measureDom.style.fontSize = dom.style.fontSize
    const endWithEmptyLine = dom.value.endsWith('\n')
    const height = parseFloat(window.getComputedStyle(measureDom).height)
    const singleLineHeight = parseFloat(window.getComputedStyle(singleLineDom).height)

    const rows = Math.round(height / singleLineHeight) + (endWithEmptyLine ? 1 : 0)

    return rows
}
