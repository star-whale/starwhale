/* eslint-disable no-param-reassign */
import axios from 'axios'

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

export function apiInit() {
    axios.interceptors.request.use((config) => {
        config.headers.Authorization = getToken()
        return config
    })
    axios.interceptors.response.use((response) => {
        if (response.headers?.authorization) setToken(response.headers.authorization)
        return typeof response.data === 'object' && 'data' in response.data ? response.data : response
    })
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
