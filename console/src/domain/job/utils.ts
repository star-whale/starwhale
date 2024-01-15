// @ts-nocheck
const SPLITER = '@@@'

function convertToRJSF(sourceJson) {
    const schema = {
        type: 'object',
        properties: {},
    }

    const uiSchema = {}

    const mapParamTypeToRJSFType = (paramType) => {
        switch (paramType) {
            case 'INT':
                return 'integer'
            case 'FLOAT':
                return 'number'
            case 'BOOL':
                return 'boolean'
            case 'STRING':
                return 'string'
            default:
                return 'string' // Default to string if param_type is not recognized
        }
    }

    const traverse = (obj, parentKey, parentIsMultiple) => {
        if (typeof obj === 'object' && obj !== null) {
            Object.keys(obj).forEach((key) => {
                const currentKey = parentKey ? `${parentKey}${SPLITER}${key}` : key
                const field = obj[key]

                if (field) {
                    const rjsfField = {
                        type: mapParamTypeToRJSFType(field.type.param_type),
                        title: field.name,
                        default: field.default,
                        required: field.required,
                    }

                    if (field.type.choices && field.type.choices?.length > 0) {
                        rjsfField.enum = field.type.choices
                    }

                    if (field.multiple || parentIsMultiple) {
                        rjsfField.type = 'array'
                        rjsfField.items = {
                            type: mapParamTypeToRJSFType(field.type.param_type),
                            default: field.default,
                        }

                        if (field.type.choices !== null && field.type.choices.length > 0) {
                            rjsfField.items.enum = field.type.choices
                            rjsfField.uniqueItems = true
                        }
                    }

                    schema.properties[currentKey] = rjsfField

                    if (field.hidden) {
                        delete uiSchema[currentKey]
                        // uiSchema[currentKey] = {
                        //     ...uiSchema[currentKey],
                        //     'ui:widget': 'hidden',
                        // }
                    }

                    if (typeof field.type.name === 'object' && field.type.name !== null) {
                        traverse(field.type.name, currentKey, field.multiple)
                    }

                    uiSchema[currentKey] = {
                        'ui:help': field.help,
                        // 'ui:field': FieldTemplate,
                    }
                }
            })
        }
    }

    sourceJson?.forEach((level1) => {
        Object.entries(level1?.arguments ?? {}).forEach(([level2Name, level2]) => {
            traverse(level2, [level1.job_name, level2Name].filter(Boolean).join(SPLITER))
        })
    })

    return { schema, uiSchema }
}

export { convertToRJSF }
