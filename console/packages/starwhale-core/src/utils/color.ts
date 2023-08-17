export const COLORS = [
    '#CC3D3D',
    '#FFB700',
    '#00b368',
    '#2b65d9',
    '#652bd9',
    '#3cade6',
    '#9fd92b',
    '#e67f17',
    '#bf268c',
    '#3dccb4',
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
    console.log(index, COLORS.length, index % COLORS.length)
    return COLORS[index % COLORS.length]
}
