import { IHasTagSchema } from '@base/schemas/resource'

export function getAlias({ tag, alias, latest }: IHasTagSchema): string[] {
    return [tag, alias, latest ? 'latest' : ''].filter(Boolean) as string[]
}

export function getAliasStr(resource: IHasTagSchema, separator = ','): string {
    return getAlias(resource).join(separator)
}
