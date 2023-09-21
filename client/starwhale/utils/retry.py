import typing as t
from inspect import iscoroutinefunction
from urllib.error import HTTPError

import requests
from tenacity import Retrying, wait_random_exponential
from tenacity.stop import stop_after_attempt
from tenacity.retry import retry_if_exception
from tenacity._asyncio import AsyncRetrying
from requests.exceptions import HTTPError as RequestsHTTPError

# https://docs.microsoft.com/en-us/azure/architecture/best-practices/retry-service-specific#general-rest-and-retry-guidelines
_RETRY_HTTP_STATUS_CODES = (408, 429, 500, 502, 503, 504)


class retry_if_http_exception(retry_if_exception):
    def __init__(self, status_codes: t.Optional[t.Sequence[int]] = None) -> None:
        self.status_codes = status_codes or _RETRY_HTTP_STATUS_CODES

        def _predicate(e: BaseException) -> bool:
            if isinstance(e, RequestsHTTPError) and isinstance(
                e.response, requests.Response
            ):
                return e.response.status_code in self.status_codes
            elif isinstance(e, HTTPError):
                return e.code in self.status_codes
            elif isinstance(e, ConnectionError):
                return True
            else:
                return False

        super().__init__(_predicate)


def http_retry(*args: t.Any, **kw: t.Any) -> t.Any:

    # support http_retry and http_retry()
    if len(args) == 1 and callable(args[0]):
        return http_retry()(args[0])
    else:

        def wrap(f: t.Callable) -> t.Any:
            _attempts = kw.pop("attempts", 10)
            _cls = AsyncRetrying if iscoroutinefunction(f) else Retrying
            return _cls(
                *args,
                reraise=True,
                stop=kw.pop("stop", stop_after_attempt(_attempts)),
                wait=kw.pop("wait", wait_random_exponential(multiplier=1, max=60)),
                retry=kw.pop(
                    "retry", retry_if_http_exception(_RETRY_HTTP_STATUS_CODES)
                ),
                **kw,
            ).wraps(f)

        return wrap
