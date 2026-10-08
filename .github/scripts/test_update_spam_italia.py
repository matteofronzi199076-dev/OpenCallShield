#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Meaningful regression cases for exact Italian feed generation (offline)."""

import contextlib
import copy
import io
import json
from pathlib import Path
import tempfile
import unittest

import update_spam_italia as feed


HEADER = "numero,categoria,prima_segnalazione,ultima_attivita,segnalazioni,note\n"
FIXED_ROW = "+390212345678,truffa,2026-07-10,2026-07-14,2,Riscontro pubblico\n"
MOBILE_ROW = "+393331234567,energia,2026-07-11,2026-07-15,4,Promozioni\n"
SHA_ORIGINAL = "a" * 40
SHA_ITALIAN = "b" * 40


def original():
    return {
        "version": "1.0",
        "updated_at": "2026-07-12",
        "annotation": {"preserve": True},
        "numbers": [
            {"number": "+34600123456", "reports": 7, "tag": "spam", "extra": "keep"},
        ],
    }


class FeedGenerationTests(unittest.TestCase):
    def test_original_values_and_extension_fields_are_preserved_without_mutation(self):
        upstream = original()
        before = copy.deepcopy(upstream)
        merged, added = feed.merge_feeds(upstream, feed.parse_italian_csv(HEADER + FIXED_ROW))
        self.assertEqual(upstream, before)
        self.assertEqual(merged["numbers"][0], before["numbers"][0])
        self.assertEqual(merged["annotation"], {"preserve": True})
        self.assertEqual(merged["version"], "1.0")
        self.assertEqual(added, 3)
        merged["annotation"]["preserve"] = False
        self.assertTrue(upstream["annotation"]["preserve"])

    def test_fixed_and_mobile_matching_forms_keep_the_fixed_leading_zero(self):
        entries = feed.parse_italian_csv(HEADER + FIXED_ROW + MOBILE_ROW)
        merged, added = feed.merge_feeds(original(), entries)
        numbers = {record["number"]: record for record in merged["numbers"]}
        self.assertEqual(added, 6)
        self.assertEqual(set(numbers) - {"+34600123456"}, {
            "+390212345678", "0212345678", "00390212345678",
            "+393331234567", "3331234567", "00393331234567",
        })
        self.assertEqual(numbers["0212345678"]["reports"], 2)
        self.assertEqual(numbers["+390212345678"]["tag"], "scam")
        self.assertEqual(numbers["3331234567"]["tag"], "telemarketing")
        self.assertNotIn("+39", numbers)
        self.assertNotIn("39", numbers)

    def test_duplicate_source_rows_and_original_collisions_never_add_reports(self):
        upstream = original()
        prior = {"number": "+390212345678", "reports": 19, "tag": "custom-original"}
        upstream["numbers"].append(prior)
        entries = feed.parse_italian_csv(HEADER + FIXED_ROW + FIXED_ROW)
        self.assertEqual(len(entries), 1)
        merged, added = feed.merge_feeds(upstream, entries)
        self.assertEqual(added, 2)
        self.assertEqual(merged["numbers"][1], prior)
        self.assertEqual(len({record["number"] for record in merged["numbers"]}), 4)
        self.assertEqual([record["reports"] for record in merged["numbers"][-2:]], [2, 2])

    def test_conflicting_duplicate_rows_fail_instead_of_estimating_counts(self):
        conflicting = FIXED_ROW.replace(",2,Riscontro", ",3,Riscontro")
        with self.assertRaises(feed.FeedError):
            feed.parse_italian_csv(HEADER + FIXED_ROW + conflicting)

    def test_short_prefix_wildcard_foreign_and_ambiguous_numbers_are_rejected(self):
        invalid_numbers = (
            "+39", "+39333", "+39021*", "+390212345678-",
            "00390212345678", "0212345678", "+34600123456",
            "+391121234567", "+39３331234567", "+390212345678 ext 1",
        )
        for number in invalid_numbers:
            with self.subTest(number=number), self.assertRaises(feed.FeedError):
                feed.parse_italian_csv(HEADER + FIXED_ROW.replace("+390212345678", number))

    def test_malformed_empty_dates_categories_and_counts_fail(self):
        invalid_csvs = (
            "", HEADER, "numero,categoria\n+390212345678,truffa\n",
            HEADER + "\n", HEADER + FIXED_ROW.replace(",2,Riscontro", ",0,Riscontro"),
            HEADER + FIXED_ROW.replace(",2,Riscontro", ",-2,Riscontro"),
            HEADER + FIXED_ROW.replace(",2,Riscontro", ",2147483648,Riscontro"),
            HEADER + FIXED_ROW.replace("truffa", "categoria_inventata"),
            HEADER + FIXED_ROW.replace("2026-07-14", "2026-02-30"),
            HEADER + FIXED_ROW.replace("2026-07-14", "2026-07-09"),
            HEADER + FIXED_ROW.replace("2026-07-10", "20260710"),
            HEADER + '"+390212345678,truffa,2026-07-10,2026-07-14,2,note\n',
        )
        for csv_text in invalid_csvs:
            with self.subTest(csv=csv_text), self.assertRaises(feed.FeedError):
                feed.parse_italian_csv(csv_text)

    def test_invalid_original_feed_is_rejected(self):
        invalid_originals = ["{", "[]", "{}"]
        for transform in (
            lambda document: document.update(numbers=[]),
            lambda document: document.update(updated_at="2026-02-30"),
            lambda document: document["numbers"][0].update(reports=True),
            lambda document: document["numbers"].append(copy.deepcopy(document["numbers"][0])),
        ):
            document = original()
            transform(document)
            invalid_originals.append(json.dumps(document))
        for text in invalid_originals:
            with self.subTest(text=text), self.assertRaises(feed.FeedError):
                feed.parse_upstream(text)

    def test_updated_at_uses_documented_source_dates_and_never_the_clock(self):
        upstream = original()
        entries = feed.parse_italian_csv(HEADER + FIXED_ROW + MOBILE_ROW)
        merged, _ = feed.merge_feeds(upstream, entries)
        self.assertEqual(merged["updated_at"], "2026-07-15")
        upstream["updated_at"] = "2026-10-07"
        merged, _ = feed.merge_feeds(upstream, entries)
        self.assertEqual(merged["updated_at"], "2026-10-07")
        entries = feed.parse_italian_csv(HEADER + FIXED_ROW.replace("2026-07-14", "2026-10-08"))
        merged, _ = feed.merge_feeds(upstream, entries)
        self.assertEqual(merged["updated_at"], "2026-10-08")

    def test_metadata_counts_unique_origins_separately_from_matching_aliases(self):
        upstream = original()
        csv_text = HEADER + FIXED_ROW + FIXED_ROW + MOBILE_ROW
        entries = feed.parse_italian_csv(csv_text)
        merged, added = feed.merge_feeds(upstream, entries)
        metadata = feed.source_metadata(
            json.dumps(upstream), csv_text, upstream, entries,
            SHA_ORIGINAL, SHA_ITALIAN, merged, added,
        )
        self.assertEqual(metadata["license"], "CC-BY-SA-4.0")
        self.assertEqual(metadata["matching"], "exact_numbers_only")
        self.assertEqual(metadata["number_of_records"], 7)
        self.assertEqual(metadata["sources"][0]["unique_numbers"], 1)
        self.assertEqual(metadata["sources"][1]["unique_numbers"], 2)
        self.assertEqual(metadata["sources"][1]["alias_records"], 6)
        self.assertEqual(metadata["sources"][1]["git_ref"], SHA_ITALIAN)
        self.assertIn("Blocklist telefonica Italia", metadata["sources"][1]["attribution"])
        self.assertIn(SHA_ORIGINAL, metadata["sources"][0]["raw_url"])
        self.assertNotIn("generated_at", metadata)

    def test_repeated_identical_offline_sync_leaves_bytes_and_mtimes_unchanged(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            upstream_path, italian_path = root / "original.json", root / "italian.csv"
            upstream_path.write_text(json.dumps(original()), encoding="utf-8")
            italian_path.write_text(HEADER + MOBILE_ROW + FIXED_ROW, encoding="utf-8")
            output = root / "output"
            arguments = [
                "--offline-upstream", str(upstream_path), "--offline-italian", str(italian_path),
                "--upstream-ref", SHA_ORIGINAL, "--italian-ref", SHA_ITALIAN,
                "--output-dir", str(output),
            ]
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(feed.main(arguments), 0)
            first = {path.name: (path.read_bytes(), path.stat().st_mtime_ns) for path in output.iterdir()}
            with contextlib.redirect_stdout(io.StringIO()) as status:
                self.assertEqual(feed.main(arguments), 0)
            second = {path.name: (path.read_bytes(), path.stat().st_mtime_ns) for path in output.iterdir()}
            self.assertEqual(first, second)
            self.assertIn("nessun file modificato", status.getvalue())

    def test_invalid_offline_source_does_not_touch_existing_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            upstream_path, italian_path = root / "original.json", root / "italian.csv"
            upstream_path.write_text(json.dumps(original()), encoding="utf-8")
            italian_path.write_text(HEADER, encoding="utf-8")
            output = root / "output"
            output.mkdir()
            for filename in ("spam_numbers.json", "sources.json"):
                (output / filename).write_text("existing valid feed\n", encoding="utf-8")
            first = {path.name: (path.read_bytes(), path.stat().st_mtime_ns) for path in output.iterdir()}
            arguments = [
                "--offline-upstream", str(upstream_path), "--offline-italian", str(italian_path),
                "--upstream-ref", SHA_ORIGINAL, "--italian-ref", SHA_ITALIAN,
                "--output-dir", str(output),
            ]
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(feed.main(arguments), 1)
            second = {path.name: (path.read_bytes(), path.stat().st_mtime_ns) for path in output.iterdir()}
            self.assertEqual(first, second)
            new_output = root / "should-not-exist"
            arguments[-1] = str(new_output)
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(feed.main(arguments), 1)
            self.assertFalse(new_output.exists())
            italian_path.unlink()
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(feed.main(arguments), 1)
            self.assertFalse(new_output.exists())

    def test_offline_reproducibility_requires_immutable_source_refs(self):
        for value in (None, "main", "abcdef", "z" * 40):
            with self.subTest(ref=value), self.assertRaises(feed.FeedError):
                feed.validate_ref(value, "source ref")
        self.assertEqual(feed.validate_ref("A" * 40, "source ref"), SHA_ORIGINAL)


if __name__ == "__main__":
    unittest.main()
