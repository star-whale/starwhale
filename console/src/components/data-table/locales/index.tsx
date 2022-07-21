// @ts-nocheck

import * as React from 'react'
import extend from 'just-extend'

import type { LocaleT } from './types'
import en_US from './en_US'

export const LocaleContext: React.Context<LocaleT> = React.createContext(en_US)

const LocaleProvider = (props: { locale: Partial<LocaleT>; children: React.ReactNode }) => {
    const { locale, children } = props
    return <LocaleContext.Provider value={extend({}, en_US, locale)}>{children}</LocaleContext.Provider>
}

export default LocaleProvider
