export const COLORS = [
    '#2b65d9',
    '#FFB700',
    '#3dccb4',
    '#CC3D3D',
    '#3cade6',
    '#652bd9',
    '#9fd92b',
    '#e67f17',
    '#bf268c',
    '#00b368',
    '#f26f55',
    '#f0b460',
    '#7676f6',
    '#73ace6',
    '#9c67e6',
    '#2e8ae6',
    '#77c88c',
    '#e4e654',
    '#ef87a1',
    '#50d9e6',
]

export function getColor(index: number) {
    return COLORS[index % COLORS.length]
}
