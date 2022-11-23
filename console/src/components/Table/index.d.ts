/// <reference types="react" />
import { TableProps as BaseTableProps } from 'baseui/table-semantic';
import { IPaginationProps } from '@/components/Table/IPaginationProps';
export interface ITableProps extends BaseTableProps {
    paginationProps?: IPaginationProps;
}
export default function Table({ isLoading, columns, data, overrides, paginationProps }: ITableProps): JSX.Element;
