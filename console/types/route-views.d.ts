// src/vite-env.d.ts
/// <reference types="vite/client" />

declare module 'virtual:route-views' {
    const routes: import('react-router-dom').RouteObject[]

    export default routes
}
