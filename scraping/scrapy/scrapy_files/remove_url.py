"""Remove the second URL column from a CSV file.

This script looks for header names that contain the substring "url"
(case-insensitive). If it finds two or more such headers it removes the
second occurrence (the one with the second index) and writes the
resulting CSV to an output file. If fewer than two URL-like columns
are found the input is copied to the output unchanged.

Usage:
  python remove_url.py input.csv
  python remove_url.py input.csv -o output.csv
  python remove_url.py input.csv --inplace   # overwrite input file

The default output is `<input_basename>_no2url.csv`.
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
from typing import List


def find_url_indices(headers: List[str]) -> List[int]:
	"""Return indices of headers that contain 'url' (case-insensitive)."""
	return [i for i, h in enumerate(headers) if 'url' in (h or '').lower()]


def remove_column_by_index_from_rows(rows, index_to_remove: int):
	for row in rows:
		# ensure row is long enough; if not, yield row unchanged
		if index_to_remove < len(row):
			yield [v for i, v in enumerate(row) if i != index_to_remove]
		else:
			yield row


def process(input_path: str, output_path: str, inplace: bool = False) -> int:
	"""Process the CSV and return number of columns removed (0 or 1)."""
	with open(input_path, newline='', encoding='utf-8') as inf:
		reader = csv.reader(inf)
		try:
			headers = next(reader)
		except StopIteration:
			# empty file
			headers = []

		url_indices = find_url_indices(headers)
		if len(url_indices) < 2:
			# nothing to remove — copy input to output
			# rewind and copy
			inf.seek(0)
			with open(output_path, 'w', newline='', encoding='utf-8') as outf:
				outf.write(inf.read())
			return 0

		# remove the second URL-like header
		remove_idx = url_indices[1]
		new_headers = [h for i, h in enumerate(headers) if i != remove_idx]

		with open(output_path, 'w', newline='', encoding='utf-8') as outf:
			writer = csv.writer(outf)
			writer.writerow(new_headers)
			for new_row in remove_column_by_index_from_rows(reader, remove_idx):
				writer.writerow(new_row)

	return 1


def main(argv=None):
	p = argparse.ArgumentParser(description='Remove the second URL column from a CSV file')
	p.add_argument('input', help='Path to input CSV file')
	p.add_argument('-o', '--output', help='Path to output CSV file')
	p.add_argument('--inplace', action='store_true', help='Overwrite input file')
	args = p.parse_args(argv)

	input_path = args.input
	if not os.path.exists(input_path):
		print(f"Input file does not exist: {input_path}", file=sys.stderr)
		return 2

	if args.inplace:
		# write to a temporary file then replace
		base, ext = os.path.splitext(input_path)
		tmp_path = base + '.tmp' + ext
		output_path = tmp_path
	else:
		if args.output:
			output_path = args.output
		else:
			base, ext = os.path.splitext(input_path)
			output_path = base + '_no2url' + ext

	removed = process(input_path, output_path, inplace=args.inplace)

	if args.inplace:
		if removed:
			os.replace(output_path, input_path)
		else:
			# nothing removed; clean up temp and leave original
			os.remove(output_path)

	if removed:
		print(f"Removed second URL-like column and wrote: {input_path if args.inplace else output_path}")
	else:
		print("No second URL-like column found; input copied to output (unchanged).")

	return 0


if __name__ == '__main__':
	raise SystemExit(main())
