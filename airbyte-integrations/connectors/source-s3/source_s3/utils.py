#
# Copyright (c) 2023 Airbyte, Inc., all rights reserved.
#

import multiprocessing as mp
import traceback
from collections.abc import Callable, Mapping
from multiprocessing import Queue
from typing import Any

import dill

from airbyte_cdk.logger import AirbyteLogger


def run_in_external_process(fn: Callable, timeout: int, max_timeout: int, logger: AirbyteLogger, args: list[Any]) -> Mapping[str, Any]:
    """Fn passed in must return a tuple of (desired return value, Exception OR None)
    This allows propagating any errors from the process up and raising accordingly
    """
    result = None
    while result is None:
        q_worker: Queue = mp.Queue()
        proc = mp.Process(
            target=multiprocess_queuer,
            # use dill to pickle the function for Windows-compatibility
            args=(dill.dumps(fn), q_worker, *args),
        )
        proc.start()
        try:
            # this attempts to get return value from function with our specified timeout up to max
            result, potential_error = q_worker.get(timeout=min(timeout, max_timeout))
        except mp.queues.Empty:  # type: ignore[attr-defined]
            if timeout >= max_timeout:  # if we've got to max_timeout and tried once with that value
                raise TimeoutError(f"Timed out too many times while running {fn.__name__}, max timeout of {max_timeout} seconds reached.")
            logger.info(f"timed out while running {fn.__name__} after {timeout} seconds, retrying...")
            timeout *= 2  # double timeout and try again
        else:
            if potential_error is None:
                return result  # type: ignore[no-any-return]
            traceback.print_exception(type(potential_error), potential_error, potential_error.__traceback__)
            raise potential_error
        finally:
            try:
                proc.terminate()
            except Exception as e:
                logger.info(f"'{fn.__name__}' proc unterminated, error: {e}")


def multiprocess_queuer(func: Callable, queue: mp.Queue, *args: Any, **kwargs: Any) -> None:
    """This is our multiprocesser helper function, lives at top-level to be Windows-compatible"""
    queue.put(dill.loads(func)(*args, **kwargs))


def get_value_or_json_if_empty_string(options: str = None) -> str:
    return options.strip() if options else "{}"
