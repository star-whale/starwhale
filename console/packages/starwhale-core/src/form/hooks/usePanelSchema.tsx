// import { RJSFSchema } from '@rjsf/utils'
// import { WidgetFactory } from '@starwhale/core/widget'
// import React from 'react'
// import useMemo from 'react'

// const schema: RJSFSchema = {
//     // title: 'My title',
//     // description: 'My description',
//     type: 'object',
//     properties: {
//         chartType: {
//             type: 'string',
//             title: 'Chart Type',
//             oneOf:
//                 WidgetFactory.getPanels().map((v) => ({
//                     const: v.type,
//                     title: v.name,
//                 })) ?? [],
//         },
//         // @ts-ignore
//         tableName: tables.length === 0 ? undefined : tableNameSchema,
//         chartTitle: {
//             title: 'Chart Title',
//             type: 'string',
//         },
//     },
// }
// export default function usePanelSchema({ tables }) {
//     const schema = useMemo(() => {
//         return {
//             type: 'object',
//             properties: {
//                 ...chartTypeField(),
//                 ...tableNameField(),
//             },
//         }
//     })
// }

export {}
