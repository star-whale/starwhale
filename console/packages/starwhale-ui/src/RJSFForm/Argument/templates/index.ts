import ArrayFieldItemTemplate from './ArrayFieldItemTemplate'
import ArrayFieldTemplate from './ArrayFieldTemplate'
// import BaseInputTemplate from "./BaseInputTemplate";
// import DescriptionField from "./DescriptionField";
// import ErrorList from "./ErrorList";
import { AddButton, MoveDownButton, MoveUpButton, RemoveButton } from './IconButton'
// import FieldErrorTemplate from "./FieldErrorTemplate";
// import FieldTemplate from './FieldTemplate'
// import SubmitButton from "./SubmitButton";
// import TitleField from "./TitleField";
// import WrapIfAdditionalTemplate from "./WrapIfAdditionalTemplate";
import { TemplatesType } from '@rjsf/utils'

const Index: Partial<TemplatesType> = {
    ArrayFieldItemTemplate,
    ArrayFieldTemplate: ArrayFieldTemplate as TemplatesType['ArrayFieldTemplate'],
    // BaseInputTemplate,
    // @ts-ignore
    ButtonTemplates: {
        AddButton,
        MoveDownButton,
        MoveUpButton,
        RemoveButton,
        // SubmitButton,
    },
    // DescriptionFieldTemplate: DescriptionField,
    // ErrorListTemplate: ErrorList,
    // FieldErrorTemplate,
    // FieldTemplate,
    // ObjectFieldTemplate,
    //   ObjectFieldTemplate as TemplatesType["ObjectFieldTemplate"],
    // TitleFieldTemplate: TitleField as TemplatesType["TitleFieldTemplate"],
    // WrapIfAdditionalTemplate,
}

export default Index
