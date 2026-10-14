# Sifry — Word Transformation Engine
## Software Architecture (SOA)

**Version:** 1.0  
**Status:** Proposed / Ready for Implementation  
**Target:** Existing Sifry application  
**Implementation:** Integrate into existing workbench architecture  
**External Reference:** https://github.com/juzerali/word-transformer

> **Repository note (2026-10-13):** the external reference repository
> **has no LICENSE file** (checked 2026-10-13; GitHub license detection
> reports none, and the repo contains no license file). The SOA's "MIT
> license" claim is therefore unverified. Reuse must be clean-room:
> reimplement the standard word-ladder algorithms (single-edit neighbor
> generation + iterative BFS) from the published concept, without copying
> the reference source. The 19-word `words.txt` fixture may be imported
> for development only with attribution, pending rights confirmation.

---

# 1. Executive Summary

Extend Sifry with a Word Transformation Engine that discovers reusable character-manipulation rules from examples and applies those rules to individual words, lists of words, or entire dictionaries.

The engine must support three primary workflows:

1. **Learn Rule:** Supply an input word and expected output word. Sifry discovers possible transformation rules and generates equivalent regular expressions where practical.
2. **Apply Rules:** Execute saved transformations against other words, optionally chaining multiple rules in a specified order.
3. **Find Word Path:** Given two words and a dictionary, discover a sequence of valid intermediate words connecting the input to the destination.

The system should support traditional word puzzles, cipher analysis, word manipulation games, and general text transformations.

The system must distinguish between a rule that *explains an example* and a rule that has been *verified against multiple examples*.

# 2. Functional Requirements

## FR-01: Learn a Rule from Two Words

The user provides:

- Input word
- Expected output word

Example:

**Input:** `WHOLE`  
**Output:** `HOLE`

Sifry analyzes the character differences and proposes candidates such as:

| Candidate | Regex | Replacement |
|---|---|---|
| Remove first character | `^.` | Empty string |
| Remove leading W | `^W` | Empty string |

The UI displays:

- Suggested rule name
- Plain-English explanation
- Regex pattern
- Replacement expression
- Number and types of edits
- Confidence/evidence assessment
- Preview of the result
- Option to save the rule

One example is insufficient to prove the intended generalization. Both candidates must remain available until further examples distinguish them.

Additional examples can be added to the same rule to narrow the candidates.

## FR-02: Supported Transformation Operations

Sifry must detect and execute:

### Deletion
- First character
- Last character
- Character at a specific position
- Character matching a value
- First or last occurrence
- Substring
- Middle N characters
- Every matching character

### Insertion
- Beginning
- End
- Specific position
- Before or after a matching character
- Before the last character
- Middle position

### Substitution
- Replace a literal character or substring
- Replace first or last character
- Replace character at a position
- Replace all matching characters
- Swap two specified character values

### Reordering
- Reverse a word
- Rotate characters
- Swap character positions
- Sort characters
- Generate anagrams

### Conditional operations
- Insert a character if absent from a specified position
- Remove it if present
- Replace only when a condition matches
- Leave unmatched words unchanged

Each operation must have a structured representation independent of regex.

## FR-03: Rule Inference

The inference engine compares input/output word pairs.

Processing stages:

1. Normalize input according to user-selected case and Unicode options.
2. Calculate one or more minimum-cost edit alignments.
3. Detect insertions, deletions, and replacements.
4. Identify special positional patterns.
5. Generate general and literal candidate rules.
6. Compile compatible rules into regex and replacement expressions.
7. Test every candidate against all supplied examples.
8. Rank candidates by simplicity, coverage, specificity, and edit cost.
9. Return candidates with explanations.

Suggested algorithmic building blocks:

- Levenshtein edit distance with backtracking
- Longest common prefix/suffix
- Longest common subsequence
- Positional character analysis
- Substring comparison
- Character multiset comparison
- Rule-template matching

For ambiguous alignments, retain multiple reasonable candidates rather than choosing arbitrarily.

A learned rule may be marked:

- Candidate
- Consistent with examples
- Contradicted by examples
- User-confirmed

Do not represent user-confirmed rules as mathematically proven.

## FR-04: Regex Generation

Rules that can be expressed accurately as a single search-and-replace operation must provide executable regex.

Examples:

| Input → Output | Regex | Replacement |
|---|---|---|
| WHOLE → HOLE | `^.` | Empty |
| CHART → CART | `^(.)(H)` | `$1` |
| SHOULD → SOULD | `^(.)(H)` | `$1` |
| FEW → NEW | `^.` | `N` |
| TINY → THINY | `^(.)(.*)$` | `$1H$2` |
| RAN → RANER | `$` | `ER` |

The interface must identify which regex dialect it generates, initially matching Sifry's active runtime.

Important:

Not every transformation can be faithfully expressed as one ordinary regex replacement.

Examples include:
- Remove the central three characters from any odd-length string.
- Swap every S with V and every V with S simultaneously.
- Toggle the presence of H at position two.

For these, store a structured operation and optionally display an equivalent multi-step regex implementation.

Do not generate misleading or semantically incorrect regex merely to satisfy the regex output requirement.

## FR-05: Apply a Transformation

The user selects a learned or manually defined rule and supplies test input.

The engine returns:

- Original word
- Transformed word
- Whether the rule matched
- Execution status
- Dictionary validity (when enabled)
- Rule identifier

Supported input sources:

- Single word
- Multiline text
- Uploaded wordlist
- Existing Sifry dictionary
- Output from another Sifry workbench

Batch results must be exportable as CSV or JSON.

## FR-06: Ordered Rule Pipelines

Users can build a named sequence of transformations.

Example pipeline:

| Step | Rule |
|---|---|
| 1 | Toggle H at second position |
| 2 | Append ER |
| 3 | Remove middle three characters |
| 4 | Insert G before final character |
| 5 | Remove final character |
| 6 | Change first character to N |
| 7 | Swap S and V |

Execute the rules strictly in the user's specified order.

Each step receives the exact output of the preceding step.

Show intermediate strings and transformations.

Allow:
- Add/remove steps
- Drag-and-drop reordering
- Enable/disable
- Step-through execution
- Reset and rerun
- Save/load pipelines
- Duplicate pipelines
- Export/import JSON

Support words that temporarily become invalid dictionary entries.

For positional operations with ambiguous positions (for example, removing the middle three characters of an even-length string), require an explicit policy such as left-center, right-center, or enumerate both.

## FR-07: Dictionary-Based Path Solver

Adapt the neighbor-generation and graph-search algorithms from:

https://github.com/juzerali/word-transformer

Given:

- Start word
- Destination word
- Dictionary

Find a sequence of dictionary words such that consecutive words differ by one insertion, deletion, or substitution.

Example:

`CAP → CAPE → APE → ATE`

Implementation:

1. Load and index the selected dictionary.
2. Validate start and destination according to configuration.
3. Generate neighboring words using single-character edits.
4. Filter candidates through dictionary membership.
5. Use iterative breadth-first search to locate a shortest path.
6. Reconstruct the path from predecessor records.
7. Display the sequence and operation at every step.

Optional future algorithms include bidirectional breadth-first search, constrained A*, and weighted transformation searches.

Dictionary-path solving must remain independent from learned-rule execution.

## FR-08: Dictionary Integration

Use Sifry's existing dictionary infrastructure.

The reference repository includes a sample `words.txt` containing 19 words. Import it as a development fixture rather than use it as the primary production dictionary.

Requirements:

- Import TXT, CSV, and JSON wordlists.
- Reuse existing dictionary datasets without deleting or reducing them.
- Deduplicate word entries.
- Support case-sensitive and case-insensitive matching.
- Support selectable character alphabets.
- Provide fast dictionary membership tests.
- Display match counts and search progress.
- Support dictionary filtering by word length or pattern.
- Preserve source attribution and any applicable license information.

Use a Set/hash index for membership checks.

Build dictionary indexes once when possible, rather than repeatedly recreating them for each transformation.

## FR-09: Anagram and Puzzle Extraction

A supplemental mode allows users to unscramble the results of transformation pipelines.

Features:

- Combine output strings from multiple sources.
- Sort and count letters.
- Generate candidate anagrams.
- Search dictionaries for matching words.
- Specify output word lengths, such as 5 + 6.
- Account for repeated characters exactly.
- Optionally filter candidate phrases by a supplied prefix, suffix, or phrase pattern.

Example:

Input: Two transformed character strings  
Constraint: Two output words, lengths 5 and 6

Return candidates without changing or discarding characters unless explicitly permitted.

This feature must remain separate from standard rule inference to avoid accidental modifications of puzzle results.

# 3. System Architecture

## 3.1 Architectural Layers

### Presentation Layer

An integrated Word Transformation workbench with three primary modes:

- Learn Rule
- Apply Rules
- Word Path Solver

An optional Anagram panel can be reached from the output/results section.

Reuse the existing Sifry layout and navigation conventions.

### Application Layer

Coordinates user input, rule inference, rule validation, execution, pipeline management, and dictionary searches.

### Domain Layer

Contains the reusable transformation algorithms and data models.

### Data Layer

Persists rules, pipelines, examples, selected dictionaries, and optional execution history.

Use the existing persistence mechanism where practical.

No mandatory cloud backend should be introduced solely for this feature.

## 3.2 Component Diagram

```text
                SIFRY UI
                    |
         Word Transformation
               Workbench
                    |
        Transformation Controller
                    |
    +---------------+---------------+
    |               |               |
 Rule Inference   Rule Executor   Word Path
    Engine           Engine        Solver
    |                |               |
 Edit Alignment    Regex /          |
    |             Structured        |
Candidate Rules    Operations       |
    |                |               |
    +--------+-------+---------------+
             |
       Dictionary Service
             |
    Existing Sifry Wordlists
             |
       Persistence Layer
       Rules / Pipelines
```

All core computation modules should be reusable outside the UI.

# 4. Data Contracts

## 4.1 Transformation Rule

```typescript
interface TransformationRule {
  id: string;
  name: string;
  description: string;

  type:
    | "regex"
    | "insert"
    | "delete"
    | "replace"
    | "swap"
    | "reorder"
    | "conditional";

  pattern?: string;
  replacement?: string;
  flags?: string;

  operation?: {
    kind: string;
    position?: number | string;
    count?: number;
    value?: string;
    parameters?: Record<string, unknown>;
  };

  examples: {
    input: string;
    output: string;
    expectedMatch?: boolean;
  }[];

  status:
    | "candidate"
    | "consistent"
    | "contradicted"
    | "confirmed";

  version: number;
}
```

## 4.2 Transformation Pipeline

```typescript
interface TransformationPipeline {
  id: string;
  name: string;
  description?: string;

  steps: {
    ruleId: string;
    enabled: boolean;
    parameters?: Record<string, unknown>;
  }[];

  options: {
    dictionaryValidation: boolean;
    stopOnError: boolean;
    preserveCase: boolean;
  };
}
```

## 4.3 Rule Inference API

```typescript
inferRules(
  examples: Array<{
    input: string;
    output: string;
  }>
): TransformationRule[]
```

## 4.4 Rule Execution API

```typescript
applyRule(
  input: string,
  rule: TransformationRule
): TransformationResult
```

## 4.5 Pipeline Execution API

```typescript
executePipeline(
  input: string,
  pipeline: TransformationPipeline
): PipelineExecutionResult
```

The result must include each executed step, intermediate string, match status, and any errors.

## 4.6 Dictionary Path API

```typescript
findWordPath(
  start: string,
  destination: string,
  dictionaryId: string,
  options?: {
    maxVisited?: number;
    maxDepth?: number;
    signal?: AbortSignal;
  }
): Promise<string[]>
```

Return an empty array when no path exists within the configured limits, with separate execution metadata distinguishing an exhausted search from a cancelled or limited search.

# 5. User Interface Design

## Mode A — Learn Rule

```text
+--------------------------------------+
| WORD TRANSFORMATION - LEARN          |
+--------------------------------------+
| Input Word       [ WHOLE          ]  |
| Expected Output  [ HOLE           ]  |
|                                      |
|         [ Discover Rules ]           |
|                                      |
| POSSIBLE RULES                       |
|                                      |
| (o) Remove first character           |
|     Regex: ^.                        |
|     Replacement: ""                  |
|                                      |
| ( ) Remove literal W at start        |
|     Regex: ^W                        |
|     Replacement: ""                  |
|                                      |
| [Add Example] [Test] [Save Rule]     |
+--------------------------------------+
```

## Mode B — Apply Rules

```text
+--------------------------------------+
| WORD TRANSFORMATION - APPLY          |
+--------------------------------------+
| Selected Rule: Remove First Letter   |
|                                      |
| Test Input: [ CHART               ]  |
|                                      |
| Result: HART                         |
|                                      |
| [Run] [Batch] [Add to Pipeline]      |
+--------------------------------------+
```

## Mode C — Pipeline

```text
+--------------------------------------+
| PIPELINE: Seven Transformers         |
+--------------------------------------+
| 1. Toggle H                          |
| 2. Append ER                         |
| 3. Remove Middle 3                   |
| 4. Insert G Before Last              |
| 5. Remove Last                       |
| 6. First Letter to N                 |
| 7. Swap S/V                          |
|                                      |
| Input: [ WORD                     ]  |
|                                      |
| [Execute] [Step] [Reset] [Save]      |
|                                      |
| Step | Input | Output                |
|  1   | ...   | ...                   |
|  2   | ...   | ...                   |
+--------------------------------------+
```

## Mode D — Word Path

```text
+--------------------------------------+
| DICTIONARY WORD PATH                 |
+--------------------------------------+
| Start:       [ CAP                ]  |
| Destination: [ ATE                ]  |
| Dictionary:  [ Select Dictionary ]  |
|                                      |
| [ Find Shortest Path ]               |
|                                      |
| CAP -> CAPE -> APE -> ATE            |
|                                      |
| Steps: 3                             |
+--------------------------------------+
```

# 6. Performance and Security

## Performance

- Use iterative graph traversal rather than recursion for large searches.
- Avoid eagerly constructing all possible word graph edges when not necessary.
- Cache dictionary indexes and invalidate them when wordlists change.
- Bound candidate enumeration for ambiguous rule inference.
- Stream or paginate large batch results.
- Use background workers when supported by Sifry's runtime.
- Support cancellation for long operations.
- Provide configurable maximum word length, candidate count, and visited-node limits.

## Security

- Treat uploaded dictionaries and regex patterns as untrusted input.
- Validate imported pipeline JSON against a versioned schema.
- Prevent unsafe regex execution and catastrophic backtracking through pattern restrictions and execution isolation or bounded runtimes.
- Restrict file imports to supported formats and sizes.
- Escape all displayed user-controlled strings.
- Avoid arbitrary code execution in structured transformation definitions.
- Keep user wordlists private unless the user explicitly exports them.

# 7. Testing Strategy

## Unit Tests

Verify each rule type independently.

Examples:

| Input | Expected | Description |
|---|---|---|
| WHOLE | HOLE | Delete first |
| CHART | CART | Delete second H |
| COUNCIL | COIL | Delete middle 3 |
| TINY | THINY | Insert H second |
| FEW | NEW | Replace first with N |
| RAN | RANER | Append ER |
| VASE | SAVE | Swap S/V |
| LOWRISE | LOSE | Delete middle 3 |

## Rule Learning Tests

- Infer first-character deletion from WHOLE/HOLE.
- Infer literal W deletion as an alternative.
- Accept additional examples.
- Eliminate candidates that fail examples.
- Retain ambiguous candidates when evidence is insufficient.
- Handle repeated letters and multiple valid edit alignments.
- Reject malformed regex safely.

## Pipeline Tests

- Verify strict rule order.
- Verify each intermediate output.
- Verify disable/enable behavior.
- Verify positional ambiguity policies.
- Verify save/load produces identical behavior.
- Verify no dictionary requirement for ordinary transformations.

## Dictionary Tests

- Verify known shortest paths.
- Handle unknown words.
- Return no path where none exists.
- Handle start equal to destination.
- Enforce search limits.
- Verify imported dictionary indexing.
- Validate cancellation behavior.

## Integration and Regression Tests

- Verify Sifry's existing screens continue functioning.
- Preserve all existing dictionaries and user data.
- Verify batch input/output workflows.
- Verify export/import compatibility.
- Verify the feature works without external network access when dictionaries are locally available.

# 8. External Code Reuse

Reference implementation:

https://github.com/juzerali/word-transformer

Relevant source files:

- `NodeFactory.js` — generation of insertion, deletion, and replacement neighbors.
- `Transformer.js` — dictionary pathfinding.
- `GraphBuilder.js` — dictionary graph construction.
- `Words.js` — reading a wordlist into a lookup structure.
- `words.txt` — sample 19-word dictionary.

Recommended approach:

Reimplement/adapt the useful algorithms within Sifry's current technology stack rather than importing the old package wholesale.

The repository declares an MIT license. Preserve copyright and license notices for any reused code and verify redistribution permissions for any external dictionary resources.

# 9. Implementation Plan

## Phase 1 — Rule Discovery

Implement:
- Two-word input
- Character alignment
- Candidate rule generation
- Regex generation
- Live rule testing
- Multiple-example validation

**Deliverable:** Users can enter WHOLE/HOLE, choose a rule, and apply it to CHART.

## Phase 2 — Pipeline Engine

Implement:
- Structured rule executor
- Ordered transformations
- Intermediate outputs
- Save/load
- JSON import/export
- Batch processing

**Deliverable:** Users can build and execute a seven-step transformation pipeline.

## Phase 3 — Dictionary Integration

Implement:
- Wordlist adapters
- Dictionary selection
- Character neighbor generation
- Iterative shortest-path search
- Performance controls

**Deliverable:** Users can find word transformation paths using selected dictionaries.

## Phase 4 — Advanced Puzzle Support

Implement:
- Anagram extraction
- Word-length constraints
- Rule suggestions from multiple examples
- Dictionary-based candidate ranking
- Combined pipeline result analysis

**Deliverable:** Users can investigate multi-stage word puzzles with optional final letter rearrangement.

# 10. Definition of Done

The SOA is successfully implemented when:

1. Two supplied words generate reasonable candidate rules.
2. Rules include regex where accurate and structured operations where necessary.
3. Users can save, edit, test, and reuse rules.
4. Multiple examples can validate or eliminate candidates.
5. Pipelines run in the correct sequence and expose intermediate outputs.
6. Dictionary searching finds valid shortest paths within configured limits.
7. Wordlists remain available and are not destructively modified.
8. Puzzle operations work without mandatory dictionary validation.
9. Batch processing, export, import, and persistence work reliably.
10. Existing Sifry functionality passes regression testing.

# 11. Recommended Git Workflow

Create a feature branch:

`feat/word-transformation-engine`

Suggested implementation commit messages:

- `feat(sifry): infer regex transformation rules from word pairs`
- `feat(sifry): add ordered word transformation pipelines`
- `feat(sifry): implement dictionary-based word path solver`
- `feat(sifry): add anagram extraction for transformed words`
- `test(sifry): cover rule inference and transformation edge cases`

**Primary feature commit:**

`feat(sifry): add word rule inference, regex transformations, and dictionary pathfinding`

---

**End of SOA — Version 1.0**
