#
# Copyright (c) 2023 Airbyte, Inc., all rights reserved.
#

import codecs
import datetime
import re
from operator import attrgetter
from pathlib import Path

import semver
from pipelines import main_logger


class Changelog:
    def __init__(self, changelog_file_path: Path) -> None:
        self.changelog_file_path = changelog_file_path
        if not changelog_file_path.exists():
            raise Exception(f"File {changelog_file_path} does not exist")
        if not changelog_file_path.is_file():
            raise Exception(f"File {changelog_file_path} is not a regular file")
        self.original_markdown_lines = changelog_file_path.read_text().splitlines()
        self.__parse_markdown()
        self.new_entries = set()

    def add_entry(self, version: semver.Version, date: datetime.date, pull_request_number: int, comment: str) -> None:
        self.new_entries.add(ChangelogEntry(date, version, pull_request_number, comment))

    def to_markdown(self) -> str:
        all_entries = self.old_entries.union(self.new_entries)
        sorted_entries = sorted(sorted(all_entries, key=attrgetter("date"), reverse=True), key=attrgetter("version"), reverse=True)
        new_lines = (
            self.original_markdown_lines[: self.changelog_entries_start_line_index]
            + [l.to_str() for l in sorted_entries]
            + self.original_markdown_lines[self.last_changelog_line_index :]
        )
        return "\n".join(new_lines) + "\n"

    def __parse_markdown(self) -> int:
        changelog_entry_re = (
            codecs.decode(r"^\| *(?P<version>[0-9]+\.[0-9+]+\.[0-9]+?) *\| *", "unicode_escape")
            + codecs.decode(r"(?P<day>[0-9]{4}-[0-9]{2}-[0-9]{2}) *\| *", "unicode_escape")
            + codecs.decode(
                r"\[?(?P<pr_number1>[0-9]*)\]? ?\(https://github.com/airbytehq/airbyte/pull/(?P<pr_number2>[0-9]*)\) *\| *",
                "unicode_escape",
            )
            + codecs.decode(r"(?P<comment>[^ ].*[^ ]) *\| *$", "unicode_escape")
        )
        changelog_header_line_index = -1
        changelog_line_enumerator = enumerate(self.original_markdown_lines)
        for line_index, line in changelog_line_enumerator:
            if re.search(rf"\| *Version *\| *Date *\| *Pull Request *\| *Subject *\|", line):
                changelog_header_line_index = line_index
                last_line = line
                break
        if changelog_header_line_index == -1:
            raise Exception("Could not find the changelog section table in the documentation file.")
        if self.original_markdown_lines[changelog_header_line_index - 1] != "":
            raise Exception("Found changelog section table in the documentation file at line but there is not blank line before it.")
        if not re.search(r"(\|-*){4}\|", next(changelog_line_enumerator)[1]):
            raise Exception("The changlog table in the documentation file is missing the header delimiter.")
        self.changelog_entries_start_line_index = changelog_header_line_index + 2

        # parse next line to see if it needs to be cut
        self.old_entries = set()
        for line_index, line in changelog_line_enumerator:
            changelog_entry_regexp = re.search(changelog_entry_re, line)
            if not changelog_entry_regexp or changelog_entry_regexp.group("pr_number1") != changelog_entry_regexp.group("pr_number2"):
                self.last_changelog_line_index = line_index
                break
            entry_version = semver.VersionInfo.parse(changelog_entry_regexp.group("version"))
            entry_date = datetime.datetime.strptime(changelog_entry_regexp.group("day"), "%Y-%m-%d").date()
            entry_pr_number = changelog_entry_regexp.group("pr_number1")
            entry_comment = changelog_entry_regexp.group("comment")
            changelog_entry = ChangelogEntry(entry_date, entry_version, entry_pr_number, entry_comment)
            self.old_entries.add(changelog_entry)


class ChangelogEntry:
    def __init__(
        self,
        date: datetime.date,
        version: semver.Version,
        pr_number: int,
        comment: str,
    ) -> None:
        self.date = date
        self.version = version
        self.pr_number = pr_number
        self.comment = comment

    def to_str(self) -> str:
        return f'| {self.version} | {self.date.strftime("%Y-%m-%d")} | [{self.pr_number}](https://github.com/airbytehq/airbyte/pull/{self.pr_number}) | {self.comment} |'

    def __repr__(self) -> str:
        return "ChangelogEntry: " + self.to_str()
