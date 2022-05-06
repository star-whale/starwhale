import axios from 'axios'
import _ from 'lodash'
import qs from 'qs'

const key = 'token'
const store = window.localStorage ?? {
    getItem: () => undefined,
    removeItem: () => undefined,
    setItem: () => undefined,
    getter: () => undefined,
}

export function apiInit() {
    axios.interceptors.request.use(function (config) {
        config.headers.Authorization = store?.token
        return config
    })
}

export const getToken = () => {
    return store?.token
}
export const setToken = (token: string | undefined) => {
    if (!token) return store.removeItem(key)
    store.setItem(key, token)
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
