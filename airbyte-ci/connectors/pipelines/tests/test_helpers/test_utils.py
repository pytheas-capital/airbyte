#
# Copyright (c) 2023 Airbyte, Inc., all rights reserved.
#

from pathlib import Path
from unittest import mock

import dagger
import pytest

from connector_ops.utils import Connector, ConnectorLanguage
from pipelines import consts
from pipelines.cli.dagger_pipeline_command import DaggerPipelineCommand
from pipelines.helpers import utils
from pipelines.helpers.connectors.modifed import get_connector_modified_files, get_modified_connectors
from tests.utils import pick_a_random_connector


@pytest.mark.parametrize(
    "ctx, expected",
    [
        (
            mock.MagicMock(
                command_path="my command path",
                obj={
                    "git_branch": "my_branch",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": None,
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_context/my_branch/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="my command path",
                obj={
                    "git_branch": "my_branch",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="my command path",
                obj={
                    "git_branch": "my_branch",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="my command path",
                obj={
                    "git_branch": "my_branch",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="my command path",
                obj={
                    "git_branch": "my_branch/with/slashes",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch_with_slashes/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="my command path",
                obj={
                    "git_branch": "my_branch/with/slashes#and!special@characters",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch_with_slashesandspecialcharacters/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="airbyte-ci command path",
                obj={
                    "git_branch": "my_branch/with/slashes#and!special@characters",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch_with_slashesandspecialcharacters/my_pipeline_start_timestamp/my_git_revision",
        ),
        (
            mock.MagicMock(
                command_path="airbyte-ci-internal command path",
                obj={
                    "git_branch": "my_branch/with/slashes#and!special@characters",
                    "git_revision": "my_git_revision",
                    "pipeline_start_timestamp": "my_pipeline_start_timestamp",
                    "ci_context": "my_ci_context",
                    "ci_job_key": "my_ci_job_key",
                },
            ),
            f"{consts.STATIC_REPORT_PREFIX}/command/path/my_ci_job_key/my_branch_with_slashesandspecialcharacters/my_pipeline_start_timestamp/my_git_revision",
        ),
    ],
)
def test_render_report_output_prefix(ctx, expected):
    assert DaggerPipelineCommand.render_report_output_prefix(ctx) == expected


@pytest.mark.parametrize("enable_dependency_scanning", [True, False])
def test_get_modified_connectors_with_dependency_scanning(all_connectors, enable_dependency_scanning):
    base_java_changed_file = Path("airbyte-cdk/java/airbyte-cdk/core/src/main/java/io/airbyte/cdk/integrations/BaseConnector.java")
    modified_files = [base_java_changed_file]

    not_modified_java_connector = pick_a_random_connector(language=ConnectorLanguage.JAVA)
    modified_java_connector = pick_a_random_connector(
        language=ConnectorLanguage.JAVA, other_picked_connectors=[not_modified_java_connector],
    )
    modified_files.append(modified_java_connector.code_directory / "foo.bar")

    modified_connectors = get_modified_connectors(modified_files, all_connectors, enable_dependency_scanning)
    if enable_dependency_scanning:
        assert not_modified_java_connector in modified_connectors
    else:
        assert not_modified_java_connector not in modified_connectors
    assert modified_java_connector in modified_connectors


def test_get_connector_modified_files():
    connector = pick_a_random_connector()
    other_connector = pick_a_random_connector(other_picked_connectors=[connector])

    all_modified_files = {
        connector.code_directory / "setup.py",
        other_connector.code_directory / "README.md",
    }

    result = get_connector_modified_files(connector, all_modified_files)
    assert result == frozenset({connector.code_directory / "setup.py"})


def test_no_modified_files_in_connector_directory():
    connector = pick_a_random_connector()
    other_connector = pick_a_random_connector(other_picked_connectors=[connector])

    all_modified_files = {
        other_connector.code_directory / "README.md",
    }

    result = get_connector_modified_files(connector, all_modified_files)
    assert result == frozenset()


@pytest.mark.anyio()
async def test_check_path_in_workdir(dagger_client):
    connector = Connector("source-openweather")
    container = (
        dagger_client.container()
        .from_("bash")
        .with_mounted_directory(str(connector.code_directory), dagger_client.host().directory(str(connector.code_directory)))
        .with_workdir(str(connector.code_directory))
    )
    assert await utils.check_path_in_workdir(container, "metadata.yaml")
    assert await utils.check_path_in_workdir(container, "setup.py")
    assert await utils.check_path_in_workdir(container, "requirements.txt")
    assert await utils.check_path_in_workdir(container, "not_existing_file") is False


def test_sh_dash_c():
    assert utils.sh_dash_c(["foo", "bar"]) == ["sh", "-c", "set -o xtrace && foo && bar"]
    assert utils.sh_dash_c(["foo"]) == ["sh", "-c", "set -o xtrace && foo"]
    assert utils.sh_dash_c([]) == ["sh", "-c", "set -o xtrace"]


@pytest.mark.anyio()
@pytest.mark.parametrize("tar_file_name", [None, "custom_tar_name.tar"])
async def test_export_container_to_tarball(mocker, dagger_client, tmp_path, tar_file_name):
    context = mocker.Mock(
        dagger_client=dagger_client,
        connector=mocker.Mock(technical_name="my_connector"),
        host_image_export_dir_path=tmp_path,
        git_revision="my_git_revision",
    )
    container = dagger_client.container().from_("bash:latest")
    platform = consts.LOCAL_BUILD_PLATFORM

    expected_tar_file_path = (
        tmp_path / f"my_connector_my_git_revision_{platform.replace('/', '_')}.tar" if tar_file_name is None else tmp_path / tar_file_name
    )
    exported_tar_file, exported_tar_file_path = await utils.export_container_to_tarball(
        context, container, platform, tar_file_name=tar_file_name,
    )
    assert exported_tar_file_path == expected_tar_file_path
    assert await exported_tar_file.size() == expected_tar_file_path.stat().st_size


@pytest.mark.anyio()
async def test_export_container_to_tarball_failure(mocker, tmp_path):
    context = mocker.Mock(
        connector=mocker.Mock(technical_name="my_connector"),
        host_image_export_dir_path=tmp_path,
        git_revision="my_git_revision",
    )

    mock_export = mocker.AsyncMock(return_value=False)
    container = mocker.AsyncMock(export=mock_export)
    platform = consts.LOCAL_BUILD_PLATFORM
    exported_tar_file, exported_tar_file_path = await utils.export_container_to_tarball(context, container, platform)
    assert exported_tar_file is None
    assert exported_tar_file_path is None

    mock_export.assert_called_once_with(
        str(tmp_path / f"my_connector_my_git_revision_{platform.replace('/', '_')}.tar"),
        forced_compression=dagger.ImageLayerCompression.Gzip,
    )
