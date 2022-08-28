/* eslint-disable no-param-reassign */
import axios from 'axios'
import qs from 'qs'

const key = 'token'
const store = window.localStorage ?? {
    getItem: () => undefined,
    removeItem: () => undefined,
    setItem: () => undefined,
    getter: () => undefined,
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
        config.headers.Authorization = store?.token
        return config
    })
    axios.interceptors.response.use(
        (response) => {
            if (response.headers.authorization) setToken(response.headers.authorization)
            return response.data?.data ? response.data : response
        },
        (error) => {
            // eslint-disable-next-line no-restricted-globals
            // eslint-disable-next-line prefer-destructuring
            const location = window.location
            if (error.response?.status === 401) {
                setToken(undefined)
            }
            if (error.response?.status === 401 && error.config.method === 'get') {
                const withUnAuthRoute =
                    ['/login', '/signup', '/create-account'].filter((path) => location.pathname.includes(path)).length >
                    0
                const search = qs.parse(location.search, { ignoreQueryPrefix: true })
                let { redirect } = search
                if (redirect && typeof redirect === 'string') {
                    redirect = decodeURI(redirect)
                } else if (!withUnAuthRoute) {
                    redirect = `${location.pathname}${location.search}`
                } else {
                    redirect = '/projects'
                }

                if (!withUnAuthRoute) {
                    location.href = `${location.protocol}//${location.host}/login?redirect=${encodeURIComponent(
                        redirect
                    )}`
                }
            }
            return error
        }
    )
}

export const getToken = () => {
    return store?.token
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
