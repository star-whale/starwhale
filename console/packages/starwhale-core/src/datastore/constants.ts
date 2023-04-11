export enum DataTypes {
    BOOL = 'BOOL',
    BYTES = 'BYTES',
    FLOAT16 = 'FLOAT16',
    FLOAT32 = 'FLOAT32',
    FLOAT64 = 'FLOAT64',
    INT16 = 'INT16',
    INT32 = 'INT32',
    INT64 = 'INT64',
    INT8 = 'INT8',
    STRING = 'STRING',
    UNKNOWN = 'UNKNOWN',
    LIST = 'LIST',
    TUPLE = 'TUPLE',
    MAP = 'MAP',
    OBJECT = 'OBJECT',
}

export enum OPERATOR {
    EQUAL = 'EQUAL',
    GREATER = 'GREATER',
    GREATER_EQUAL = 'GREATER_EQUAL',
    LESS = 'LESS',
    LESS_EQUAL = 'LESS_EQUAL',
    NOT = 'NOT',
    // IN = 'IN',
    // NOT_IN = 'NOT_IN',
    // CONTAINS = 'CONTAINS',
    // NOT_CONTAINS = 'NOT_CONTAINS',
    // IS = 'IS',
    // IS_NOT = 'IS_NOT',
    EXISTS = 'EXISTS',
    NOT_EXISTS = 'NOT_EXISTS',
}
