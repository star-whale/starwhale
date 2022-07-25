from loguru import logger

import inspect

from starwhale.core.job.base.model import Context
from starwhale.api.job import step


# class TestStep:
@step(
    resources="gpu=1,cpu=2",
    concurrency=1,
    task_num=2
)
def evaluate_ppl(_context: Context):
    """
    step 'ppl' demo
    :param _context: common param
    """
    logger.debug("test ppl")
    # store.set(f"xxx/{context['id']}")
    # return {'eval_res': results, 'label': label}


@step(
    resources="cpu=1",
    concurrency=1,
    dependency='evaluate_ppl'
)
def evaluate_cmp(_context: Context):
    """
        step 'cmp' demo
        :param _context: common param
        """
    logger.debug("test cmp")


@step(
    job_name='second',
    resources="gpu=1,cpu=2",
    concurrency=1,
    task_num=2
)
def evaluate_ppl2(_context: Context):
    """
    step 'ppl' demo
    :param _context: common param
    """
    logger.debug("test ppl2")
    # store.set(f"xxx/{context['id']}")
    # return {'eval_res': results, 'label': label}


@step(
    job_name='second',
    resources="cpu=1",
    concurrency=1,
    dependency='evaluate_ppl2'
)
def evaluate_cmp2(_context: Context):
    """
        step 'cmp' demo
        :param _context: common param
        """
    logger.debug("test cmp2")
