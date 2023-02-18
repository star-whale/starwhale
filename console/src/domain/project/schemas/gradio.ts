export interface ICompomentProps {
    components?: string[]
    samples?: string[][]
}

export interface IComponent {
    id: number
    type: string
    props?: ICompomentProps
}

export interface IDependency {
    targets: number[]
    trigger: string
    backend_fn: boolean
    js: string
}

export interface IGradioConfig {
    version: string
    components: IComponent[]
    dependencies: IDependency[]
}
