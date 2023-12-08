#
# Copyright (c) 2023 Airbyte, Inc., all rights reserved.
#

"""The actions package is made to declare reusable pipeline components."""

from __future__ import annotations

from typing import TYPE_CHECKING, Union

import asyncer

from pipelines.models.steps import Step, StepStatus

if TYPE_CHECKING:
    from pipelines.models.steps import StepResult


async def run_steps(
    steps_and_run_args: list[Union[Step, tuple[Step, tuple]] | list[Union[Step, tuple[Step, tuple]]]], results: list[StepResult] = [],
) -> list[StepResult]:
    """Run multiple steps sequentially, or in parallel if steps are wrapped into a sublist.

    Args:
        steps_and_run_args (List[Union[Step, Tuple[Step, Tuple]] | List[Union[Step, Tuple[Step, Tuple]]]]): List of steps to run, if steps are wrapped in a sublist they will be executed in parallel. run function arguments can be passed as a tuple along the Step instance.
        results (List[StepResult], optional): List of step results, used for recursion.

    Returns:
        List[StepResult]: List of step results.
    """
    # If there are no steps to run, return the results
    if not steps_and_run_args:
        return results

    # If any of the previous steps failed, skip the remaining steps
    if any(result.status is StepStatus.FAILURE for result in results):
        skipped_results = []
        for step_and_run_args in steps_and_run_args:
            if isinstance(step_and_run_args, tuple):
                skipped_results.append(step_and_run_args[0].skip())
            else:
                skipped_results.append(step_and_run_args.skip())
        return results + skipped_results

    # Pop the next step to run
    steps_to_run, remaining_steps = steps_and_run_args[0], steps_and_run_args[1:]

    # wrap the step in a list if it is not already (allows for parallel steps)
    if not isinstance(steps_to_run, list):
        steps_to_run = [steps_to_run]

    async with asyncer.create_task_group() as task_group:
        tasks = []
        for step in steps_to_run:
            if isinstance(step, Step):
                tasks.append(task_group.soonify(step.run)())
            elif isinstance(step, tuple) and isinstance(step[0], Step) and isinstance(step[1], tuple):
                step, run_args = step
                tasks.append(task_group.soonify(step.run)(*run_args))

    new_results = [task.value for task in tasks]

    return await run_steps(remaining_steps, results + new_results)
