import { IHasTagSchema } from '@base/schemas/resource'

export function getAlias({ tags, alias, latest }: IHasTagSchema): string[] {
    return [alias, latest ? 'latest' : '', ...(tags ?? [])].filter(Boolean) as string[]
}

export function getAliasStr(resource: IHasTagSchema, separator = ','): string {
    return getAlias(resource).join(separator)
}
