import _ from 'lodash'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const isPromise = (obj: any) =>
    !!obj && (typeof obj === 'object' || typeof obj === 'function') && typeof obj.then === 'function'

export function millisecondsToTimeStr(milliseconds: number): string {
    if (milliseconds < 0) return '-'
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
    if (bytes < 1024) {
        return `${bytes} B`
    }

    if (bytes < 1024 * 1024) {
        const k = bytes / 1024
        const intKi = Math.round(k)
        if (k === intKi) {
            return `${intKi} KB`
        }
        return `${k.toFixed(2)} KB`
    }

    const mi = bytes / 1024 / 1024
    if (mi > 1024 * 1024 * 1024) {
        const pi = mi / 1024 / 1024 / 1024
        const intPi = Math.round(pi)
        if (pi === intPi) {
            return `${intPi} PB`
        }
        return `${pi.toFixed(2)} PB`
    }
    if (mi > 1024 * 1024) {
        const ti = mi / 1024 / 1024
        const intTi = Math.round(ti)
        if (ti === intTi) {
            return `${intTi} TB`
        }
        return `${ti.toFixed(2)} TB`
    }
    if (mi > 1024) {
        const gi = mi / 1024
        const intGi = Math.round(gi)
        if (gi === intGi) {
            return `${intGi} GB`
        }
        return `${gi.toFixed(2)} GB`
    }
    return `${mi.toFixed(2)} MB`
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

export function longestCommonSubstring(string1: string, string2: string) {
    // Convert strings to arrays to treat unicode symbols length correctly.
    // For example:
    // '𐌵'.length === 2
    // [...'𐌵'].length === 1
    const s1 = string1.split('')
    const s2 = string2.split('')

    // Init the matrix of all substring lengths to use Dynamic Programming approach.
    const substringMatrix = Array(s2.length + 1)
        .fill(null)
        .map(() => {
            return Array(s1.length + 1).fill(null)
        })

    // Fill the first row and first column with zeros to provide initial values.
    for (let columnIndex = 0; columnIndex <= s1.length; columnIndex += 1) {
        substringMatrix[0][columnIndex] = 0
    }

    for (let rowIndex = 0; rowIndex <= s2.length; rowIndex += 1) {
        substringMatrix[rowIndex][0] = 0
    }

    // Build the matrix of all substring lengths to use Dynamic Programming approach.
    let longestSubstringLength = 0
    let longestSubstringColumn = 0
    let longestSubstringRow = 0

    for (let rowIndex = 1; rowIndex <= s2.length; rowIndex += 1) {
        for (let columnIndex = 1; columnIndex <= s1.length; columnIndex += 1) {
            if (s1[columnIndex - 1] === s2[rowIndex - 1]) {
                substringMatrix[rowIndex][columnIndex] = substringMatrix[rowIndex - 1][columnIndex - 1] + 1
            } else {
                substringMatrix[rowIndex][columnIndex] = 0
            }

            // Try to find the biggest length of all common substring lengths
            // and to memorize its last character position (indices)
            if (substringMatrix[rowIndex][columnIndex] > longestSubstringLength) {
                longestSubstringLength = substringMatrix[rowIndex][columnIndex]
                longestSubstringColumn = columnIndex
                longestSubstringRow = rowIndex
            }
        }
    }

    if (longestSubstringLength === 0) {
        // Longest common substring has not been found.
        return ''
    }

    // Detect the longest substring from the matrix.
    let longestSubstring = ''

    while (substringMatrix[longestSubstringRow][longestSubstringColumn] > 0) {
        longestSubstring = s1[longestSubstringColumn - 1] + longestSubstring
        longestSubstringRow -= 1
        longestSubstringColumn -= 1
    }
    return longestSubstring
}

export function parseDecimal(v: number, decimal: number): string {
    if (v === 0) return String(v)
    if (v.toString().indexOf('.') === -1) return String(v)
    return Number(v).toFixed(decimal)
}

export function expandBorder(width?: string, weight?: string, color?: string) {
    return {
        ...(width
            ? {
                  borderTopWidth: width,
                  borderBottomWidth: width,
                  borderRightWidth: width,
                  borderLeftWidth: width,
              }
            : {}),
        ...(weight
            ? {
                  borderLeftStyle: weight,
                  borderRightStyle: weight,
                  borderTopStyle: weight,
                  borderBottomStyle: weight,
              }
            : {}),
        ...(color
            ? {
                  borderLeftColor: color,
                  borderRightColor: color,
                  borderTopColor: color,
                  borderBottomColor: color,
              }
            : {}),
    } as Record<string, string>
}

export function expandBorderRadius(radius: string) {
    return {
        borderTopLeftRadius: radius,
        borderTopRightRadius: radius,
        borderBottomRightRadius: radius,
        borderBottomLeftRadius: radius,
    }
}

// eslint-disable-next-line  @typescript-eslint/no-unused-vars
export function expandPadding(top: string, right: string, bottom: string, left: string) {
    return {
        paddingTop: top ?? undefined,
        paddingBottom: bottom ?? undefined,
        paddingLeft: left ?? undefined,
        paddingRight: right ?? undefined,
    }
}

// eslint-disable-next-line  @typescript-eslint/no-unused-vars
export function expandMargin(top: string, right: string, bottom: string, left: string) {
    return {
        marginTop: top ?? undefined,
        marginBottom: bottom ?? undefined,
        marginLeft: left ?? undefined,
        marginRight: right ?? undefined,
    }
}
