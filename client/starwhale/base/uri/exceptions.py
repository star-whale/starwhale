from typing import List, Optional


class NoMatchException(Exception):
    def __init__(self, item: str, found: Optional[List[str]] = None) -> None:
        message = f"Can not find the exact match item {item}, found: {found}"
        super().__init__(message)


class MultipleMatchException(Exception):
    def __init__(self, item: str, found: Optional[List[str]] = None) -> None:
        message = f"Found multiple match item {item}, found: {found}"
        super().__init__(message)


class VerifyException(Exception):
    def __init__(self, msg: str = "") -> None:
        super().__init__(msg)


class UriTooShortException(Exception):
    def __init__(self, expect: int, get: int, msg: str = "") -> None:
        super().__init__(
            ", ".join([f"URI too short, expect {expect} parts, get {get}", msg])
        )
