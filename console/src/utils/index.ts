import _ from 'lodash'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const isPromise = (obj: any) =>
    !!obj && (typeof obj === 'object' || typeof obj === 'function') && typeof obj.then === 'function'

export function millisecondsToTimeStr(milliseconds: number): string {
    let result = ''
    let temp = Math.floor(milliseconds / 1000)
    const hours = Math.floor(temp / 3600)
    if (hours) {
        result = `${hours}h`
    }
    const minutes = Math.floor((temp %= 3600) / 60)
    if (minutes) {
        result = `${result}${minutes}m`
    }
    const seconds = temp % 60
    if (seconds) {
        result = `${result}${seconds}s`
    }
    return result
}

export function getCookie(name: string): string {
    const regex = new RegExp(`(?:(?:^|.*;*)${name}*=*([^;]*).*$)|^.*$`)
    const global = typeof document !== 'undefined' ? document : null
    return global ? global.cookie.replace(regex, '$1') : ''
}

export function simulationJump(href: string, download?: string) {
    const a = document.createElement('a')
    a.href = href
    document.body.appendChild(a)

    if (download !== undefined) {
        a.download = download
    }

    a.click()
    document.body.removeChild(a)
}

export function formatCommitId(s: string): string {
    return s.slice(0, 7)
}

export function popupWindow(url: string, windowName: string, width = 800, height = 600) {
    const newWindow = window.open(url, windowName, `width=${width},height=${height}`)
    newWindow?.focus()
    return newWindow
}

export function processUrl(url: string) {
    let newUrl = url
    const pieces = url.split('://')
    if (pieces.length > 1) {
        newUrl = `//${pieces[1]}`
    }
    return newUrl
}

export function sizeStrToByteNum(sizeStr?: string): number | undefined {
    if (sizeStr === undefined) {
        return undefined
    }
    const m = sizeStr.match(/(\d+)(m|ki|mi|gi|ti|pi|ei)$/i)
    let num
    if (!m) {
        return num
    }
    const [, numStr, unit] = m
    num = parseInt(numStr, 10)
    switch (unit.toLowerCase()) {
        case 'm':
            return num * 1000 * 1000
        case 'ki':
            return num * 1024
        case 'mi':
            return num * 1024 * 1024
        case 'gi':
            return num * 1024 * 1024 * 1024
        case 'ti':
            return num * 1024 * 1024 * 1024 * 1024
        case 'pi':
            return num * 1024 * 1024 * 1024 * 1024 * 1024
        case 'ei':
            return num * 1024 * 1024 * 1024 * 1024 * 1024 * 1024
        default:
            return num
    }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function isArrayModified(orig: Array<any>, cur: Array<any>): boolean {
    if (orig.length !== cur.length) {
        return true
    }
    return cur.some((cv, idx) => {
        const ov = orig[idx]
        if (_.isArray(cv)) {
            if (!_.isArray(ov)) {
                return true
            }
            return isArrayModified(ov, cv)
        }
        if (_.isObject(cv)) {
            if (!_.isObject(ov)) {
                return true
            }
            // eslint-disable-next-line @typescript-eslint/no-use-before-define
            return isModified(ov, cv)
        }
        return ov !== cv
    })
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function isModified(orig?: Record<string, any>, cur?: Record<string, any>): boolean {
    if (!orig || !cur) {
        return orig !== cur
    }

    return Object.keys(cur).some((k) => {
        const ov = orig[k]
        const cv = cur[k]
        if (_.isArray(cv)) {
            if (!_.isArray(ov)) {
                return true
            }
            return isArrayModified(ov, cv)
        }
        if (_.isObject(cv)) {
            if (!_.isObject(ov)) {
                return true
            }
            return isModified(ov, cv)
        }
        if (ov === undefined && ['', null, 0, false].indexOf(cv) >= 0) {
            return false
        }
        return ov !== cv
    })
}

export function getMilliCpuQuantity(value?: string): number {
    if (!value) {
        return 0
    }
    const m = value.match(/(\d+)m/)
    if (m) {
        // eslint-disable-next-line prefer-destructuring
        return parseInt(m[1], 10)
    }
    const m0 = value.match(/(\d+)/)
    if (m0) {
        // eslint-disable-next-line prefer-destructuring
        return parseInt(m0[1], 10) * 1000
    }
    return 0
}

export function getCpuCoresQuantityStr(value: number): string {
    const cores = value / 1000
    const intCores = Math.round(cores)
    if (cores === intCores) {
        return String(intCores)
    }
    return cores.toFixed(2)
}

export function getReadableStorageQuantityStr(bytes?: number): string {
    if (bytes === undefined) {
        return ''
    }
    const mi = bytes / 1024 / 1024
    if (mi > 1024 * 1024 * 1024) {
        const pi = mi / 1024 / 1024 / 1024
        const intPi = Math.round(pi)
        if (pi === intPi) {
            return `${intPi} Pi`
        }
        return `${pi.toFixed(2)} Pi`
    }
    if (mi > 1024 * 1024) {
        const ti = mi / 1024 / 1024
        const intTi = Math.round(ti)
        if (ti === intTi) {
            return `${intTi} Ti`
        }
        return `${ti.toFixed(2)} Ti`
    }
    if (mi > 1024) {
        const gi = mi / 1024
        const intGi = Math.round(gi)
        if (gi === intGi) {
            return `${intGi} Gi`
        }
        return `${gi.toFixed(2)} Gi`
    }
    return `${mi} Mi`
}

export function numberToPercentStr(v: number): string {
    return `${(v * 100).toFixed(2)}%`
}

export function flattenObject(o: any, prefix = '', result: any = {}, keepNull = true) {
    if (_.isString(o) || _.isNumber(o) || _.isBoolean(o) || (keepNull && _.isNull(o))) {
        /* eslint-disable no-param-reassign */
        result[prefix] = o
        return result
    }

    if (_.isArray(o) || _.isPlainObject(o)) {
        Object.keys(o).forEach((i) => {
            let pref = prefix
            if (_.isArray(o)) {
                pref += `[${i}]`
            } else if (_.isEmpty(prefix)) {
                pref = i
            } else {
                pref = `${prefix} / ${i}`
            }
            flattenObject(o[i] ?? {}, pref, result, keepNull)
        })
        return result
    }
    return result
}
