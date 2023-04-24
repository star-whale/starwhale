// src/vite-env.d.ts
/// <reference types="vite/client" />

declare module 'virtual:route-views' {
    export type IExtendRoutesType = {
        auth?: boolean
        routes?: IRoute[]
    }

    export type IRoute = {
        path?: string
        from?: string
        to?: string
        component?: any
        routes?: IRoute[]
    }

    // eslint-disable-next-line
    const component: IExtendRoutesType[]
    // console.log(component)
    export default component
}
