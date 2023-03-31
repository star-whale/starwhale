import yaml from 'js-yaml'

export function getMeta(meta: string): { revision?: string; rows?: number } {
    try {
        const obj: any = yaml.load(meta)
        return {
            revision: obj?.data_datastore_revision,
            rows: obj?.dataset_summary?.rows ?? 0,
        }
    } catch (e) {}
    return {}
}
