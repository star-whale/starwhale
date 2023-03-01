// extend resource: https://github.com/KAROTT7/vite-plugin-react-views

import fs from 'node:fs'
import path from 'node:path'
import type { PluginOption, ResolvedConfig } from 'vite'
type RouteObject = any

interface Options {
    dir?: string
    exclude?(path: string): boolean
    sync?(path: string): boolean
    extensions?: string[]
}

function slash(id: string) {
    return id.replace(/\\/g, '/')
}

function readContent(id: string) {
    return fs.readFileSync(id).toString().trim()
}

function join(...rest: string[]) {
    return slash(path.join(...rest))
}

function VitePluginReactRouter(opts: Options = {}): PluginOption {
    const { dir = 'src/pages', exclude, sync, extensions = ['js', 'jsx', 'ts', 'tsx'] } = opts

    let _config: ResolvedConfig
    const ROUTE_RE = new RegExp(`routes\\.(${extensions.join('|')})$`)
    const MODULE_NAME = 'virtual:route-views'
    /**
     * Do not add '\0' prefix, the jsx file need to
     * be transformed by @vitejs/plugin-react@^3
     */
    const VIRTUAL_MODULE = MODULE_NAME + `.${extensions[1]}`
    const emptyFiles = new Set()
    const nonEmptyFiles = new Set()

    function createRoutes(folder: string) {
        const originFolder = join(_config.root!, dir)
        const originFolderStat = fs.statSync(originFolder)

        if (!originFolderStat.isDirectory()) {
            throw new Error(`${folder} must be a folder.`)
        }

        const syncRoutesMap = new Map()
        let loadingId = ''
        const routes: RouteObject[] = [
            // {
            //     path: '/',
            //     children: [],
            // },
        ]

        function normalizedFileName(id: string) {
            const index = id.lastIndexOf('.')
            return index > -1 ? id.slice(0, index) : id
        }

        function getRouteElement(id: string, syncRoute = false) {
            if (syncRoute) {
                const name = id
                    .slice(originFolder.length, id.indexOf(path.extname(id)))
                    .split('/')
                    .map((segment) => {
                        if (segment === '404') {
                            return 'NoMatch'
                        } else if (segment) {
                            return segment.charAt(0).toUpperCase() + segment.slice(1).replace(/[^a-zA-Z]+/g, '')
                        }

                        return segment
                    })
                    .join('')

                syncRoutesMap.set(id, name)

                return '<' + name + ' />'
            }

            return `Lazilize(() => import('${id}'))`
        }

        function getRouteList(id: string) {
            const name = id
                .slice(originFolder.length, id.indexOf(path.extname(id)))
                .split('/')
                .map((segment) => {
                    if (segment === '404') {
                        return 'NoMatch'
                    } else if (segment) {
                        return segment.charAt(0).toUpperCase() + segment.slice(1).replace(/[^a-zA-Z]+/g, '')
                    }

                    return segment
                })
                .join('')

            syncRoutesMap.set(id, name)

            return name
        }

        function readFiles(id: string, route: RouteObject, isDirectory = false, root = false) {
            if (exclude?.(id)) return

            const basename = id.endsWith(dir) ? '/' : path.basename(id)

            if (isDirectory) {
                const files = fs.readdirSync(id)

                files.forEach((file) => {
                    const nextFile = join(id, file)
                    if (exclude?.(nextFile)) return

                    const stat = fs.statSync(nextFile)
                    if (stat.isDirectory()) {
                        const newRoute = { path: path.basename(nextFile) } as RouteObject
                        // ;(route.children || (route.children = [])).push(newRoute)
                        readFiles(nextFile, newRoute, true, false)
                    } else {
                        readFiles(nextFile, route, false, basename === '/')
                    }
                })
            } else if (ROUTE_RE.test(basename)) {
                const content = readContent(id)
                if (!content) {
                    emptyFiles.add(id)
                    return
                }

                nonEmptyFiles.add(id)
                const plainBaseName = normalizedFileName(basename)
                const isSync = !!sync?.(id)

                routes.push(getRouteList(id))
            }
        }

        readFiles(originFolder, routes[0]!, true, true)

        return { routes, loadingId, syncRoutesMap }
    }

    return {
        name: 'vite-plugin-react-views',
        configResolved(c) {
            _config = c
        },
        configureServer(server) {
            function handleFileChange(path: string) {
                if (slash(path).includes(dir) && !exclude?.(path)) {
                    const mod = server.moduleGraph.getModuleById(VIRTUAL_MODULE)
                    if (mod) {
                        server.moduleGraph.invalidateModule(mod)
                    }

                    server.ws.send({
                        type: 'full-reload',
                        path: '*',
                    })
                }
            }

            server.watcher.on('add', handleFileChange)
            server.watcher.on('unlink', handleFileChange)
            server.watcher.on('change', (path) => {
                const content = readContent(path)
                if (emptyFiles.has(path) && content) {
                    emptyFiles.delete(path)
                    nonEmptyFiles.add(path)
                    handleFileChange(path)
                } else if (nonEmptyFiles.has(path) && !content) {
                    emptyFiles.add(path)
                    nonEmptyFiles.delete(path)
                    handleFileChange(path)
                }
            })
        },
        resolveId(id: string) {
            if (id === MODULE_NAME) {
                return VIRTUAL_MODULE
            }
        },
        load(id) {
            if (id === VIRTUAL_MODULE) {
                const { routes, loadingId, syncRoutesMap } = createRoutes(dir)
                let syncRouteString = ''
                let syncRouteExportString = ''
                let num = 0
                syncRoutesMap.forEach((value, key) => {
                    num++
                    syncRouteString += `import ${value}_${num} from '${key}'\n`
                    syncRouteExportString += `...${value}_${num},\n`
                })

                return `
${syncRouteString}

export default [
    ${syncRouteExportString}
]
                `
            }
        },
    }
}

export default VitePluginReactRouter
