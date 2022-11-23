import * as i18next from 'i18next';
import { locales } from '@/i18n/locales';
export declare type Translator = (key: keyof typeof locales, options?: {
    [key: string]: any;
}) => string;
export declare type UseTranslationResponse = [Translator, i18next.i18n];
export default function useTranslation(): UseTranslationResponse;
