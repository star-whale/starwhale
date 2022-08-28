import yaml from 'js-yaml'

export function getMetaRow(config: string) {
    const meta: any = yaml.load(config)
    return meta?.dataset_summary?.rows ?? 0
}
