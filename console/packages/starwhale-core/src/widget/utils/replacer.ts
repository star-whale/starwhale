// @ts-nocheck
import produce from 'immer'
import _ from 'lodash'

export type Matcher = {
    matcher: RegExp
    injectKey: string
    property: string
    isSave: true
}
export const DATA_BIND_REGEX = /{{([\s\S]*?)}}/
export const DATA_BIND_REGEX_GLOBAL = /{{([\s\S]*?)}}/g
export const AUTOCOMPLETE_MATCH_REGEX = /{{\s*.*?\s*}}/g
export const QUOTED_BINDING_REGEX = /["']({{[\s\S]*?}})["']/g
export const PANEL_DYNAMIC_MATCHES: Matcher[] = [
    {
        matcher: /(^project\/.*?\/.*?\/.*?\/.*?\/)/,
        injectKey: 'prefix',
        property: 'fieldConfig.data.tableName',
        isSave: true,
    },
]
export const isDynamicValue = (value: string): boolean => DATA_BIND_REGEX.test(value)

export type Replacer = ReturnType<typeof replacer>

export const replacer = (matches: Matcher[]) => {
    return {
        toTemplate: (raw: any, isSaveInjectVars = false) => {
            let data: any = raw

            matches.forEach((m) => {
                const { matcher, injectKey, property, isSave } = m
                const rawValue = _.get(raw, property, '')
                if (rawValue && !_.isObject(rawValue) && rawValue.match(matcher)) {
                    const rawReplaced = rawValue.match(matcher)?.[0]
                    const replaced = rawValue.replace(matcher, `{{${injectKey}}}`)
                    data = produce(data, (temp: any) => {
                        _.set(temp, property, replaced)
                        if (isSave && isSaveInjectVars) {
                            _.set(temp, `dynamicConfig.data.${injectKey}`, rawReplaced)
                        }
                    })
                }
            })

            return data
        },
        toOrigin: (raw: any, injected: any) => {
            let data: any = raw
            matches.forEach((m) => {
                const { injectKey, property } = m
                const rawValue = _.get(raw, property)
                if (isDynamicValue(rawValue) && injected?.[injectKey]) {
                    const origin = rawValue.replace(`{{${injectKey}}}`, injected[injectKey])

                    data = produce(data, (temp: any) => {
                        _.set(temp, property, origin)
                    })
                }
            })

            return data
        },
    }
}

export function getDynamicStringSegments(dynamicString: string): string[] {
    let stringSegments = []
    const indexOfDoubleParanStart = dynamicString.indexOf('{{')
    if (indexOfDoubleParanStart === -1) {
        return [dynamicString]
    }

    const firstString = dynamicString.substring(0, indexOfDoubleParanStart)
    // eslint-disable-next-line @typescript-eslint/no-unused-expressions
    firstString && stringSegments.push(firstString)
    let rest = dynamicString.substring(indexOfDoubleParanStart, dynamicString.length)

    let sum = 0
    for (let i = 0; i <= rest.length - 1; i++) {
        const char = rest[i]
        const prevChar = rest[i - 1]

        if (char === '{') {
            sum++
        } else if (char === '}') {
            sum--
            if (prevChar === '}' && sum === 0) {
                stringSegments.push(rest.substring(0, i + 1))
                rest = rest.substring(i + 1, rest.length)
                if (rest) {
                    stringSegments = stringSegments.concat(getDynamicStringSegments(rest))
                    break
                }
            }
        }
    }
    if (sum !== 0 && dynamicString !== '') {
        return [dynamicString]
    }
    return stringSegments
}
