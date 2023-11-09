/* eslint-disable no-param-reassign */
import axios from 'axios'
import { Api } from './server/Api'
import { HttpClient } from './server/http-client'

let client
// eslint-disable-next-line
let api

const key = 'token'
const store = window.localStorage ?? {
    getItem: () => undefined,
    removeItem: () => undefined,
    setItem: () => undefined,
    getter: () => undefined,
}

export const getToken = () => {
    return store?.token
}

export const setToken = (token: string | undefined) => {
    if (!token) {
        store.removeItem(key)
        return
    }
    store.setItem(key, token)
}

export function apiInit(simple = false) {
    const requestToken = (config) => {
        config.headers.Authorization = simple ? '' : getToken()
        return config
    }
    const responseParse = (response) => {
        if (response.headers?.authorization && !simple) setToken(response.headers.authorization)
        return typeof response.data === 'object' && 'data' in response.data ? response.data : response
    }

    axios.interceptors.request.use(requestToken)
    axios.interceptors.response.use(responseParse)

    client = new HttpClient()
    client.instance.interceptors.request.use(requestToken)
    client.instance.interceptors.response.use(responseParse)
    api = new Api(client)
}

export function getErrMsg(err: any): string {
    if (err.response && err.response.data) {
        const msg = err.response.data.message
        if (msg) {
            return msg
        }
        const errStr = err.response.data.error
        if (errStr) {
            return errStr
        }
        return err.response.statusText
    }
    return String(err)
}

export * from './server/data-contracts'
export { api }
