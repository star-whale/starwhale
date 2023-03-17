from ._impl.evaluation import predict, evaluate, PipelineHandler, EvaluationLogStore

log = EvaluationLogStore.log
log_summary = EvaluationLogStore.log_summary
iter = EvaluationLogStore.iter

__all__ = [
    "PipelineHandler",
    "predict",
    "evaluate",
    "log",
    "log_summary",
    "iter",
]
