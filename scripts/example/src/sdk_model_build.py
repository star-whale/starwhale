import typing as t
from pathlib import Path

from starwhale import model, Context, handler, fine_tune, pass_context

try:
    from .evaluator import predict
except ImportError:
    from evaluator import predict


ROOTDIR = Path(__file__).parent.parent


@handler(replicas=1)
@pass_context
def context_handle(ctx: Context) -> t.Any:
    print(ctx)


@fine_tune
def ft(train_datasets: t.List) -> None:
    ...


model.build(name="ctx_handle", modules=[context_handle, predict, ft], workdir=ROOTDIR)

model.build(name="ctx_handle_no_modules", workdir=ROOTDIR)
