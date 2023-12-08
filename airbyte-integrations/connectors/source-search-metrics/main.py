#
# Copyright (c) 2023 Airbyte, Inc., all rights reserved.
#


import sys

from source_search_metrics import SourceSearchMetrics

from airbyte_cdk.entrypoint import launch

if __name__ == "__main__":
    source = SourceSearchMetrics()
    launch(source, sys.argv[1:])
