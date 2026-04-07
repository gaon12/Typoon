# History Search Behavior Spec

## Scope
- Target: `HistoryRepository.searchHistory`
- Data sources: FTS match + SQL LIKE fallback + choseong (initial consonant) filter

## Query Normalization
- Control characters are stripped.
- Leading/trailing spaces are trimmed.
- Multiple spaces are collapsed into a single space.

## Matching Policy
- Empty query:
  - Returns recent history (`LIMIT 50`) sorted by newest first.
- Choseong-only query (for example, `ㅎㄱ`, `ㄱㅅ`):
  - Uses recent history window (`LIMIT 500`) and filters by initial consonant sequence from both source and result text.
  - Returns newest 50 matches.
- General query:
  - Builds a safe FTS prefix query from alphanumeric/Hangul tokens.
  - Runs SQL LIKE fallback in parallel for robustness.
  - Merges FTS + LIKE results, removes duplicates by id, sorts by newest first, returns 50.

## Stability Rules
- FTS input is tokenized and wrapped as quoted-prefix terms (for example, `"token"*`), joined with `AND`.
- Special characters that can break LIKE (`%`, `_`, `\`) are escaped before query execution.
- When no valid FTS token exists, LIKE-only path is used.

## Expected UX
- Korean/English/numeric mixed input should return predictable results.
- Special-character-heavy input should not crash search and should degrade gracefully.
- Choseong queries should find likely Korean targets without forcing exact full-syllable matches.
