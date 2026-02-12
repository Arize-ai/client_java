# Changelog

## [2.2.6](https://github.com/Arize-ai/arize/compare/arize-java-sdk/v2.2.5...arize-java-sdk/v2.2.6) (2026-02-12)

### 🐛 Bug Fixes

- set string label as category object instead of scorecategory in java sdk ([#62005](https://github.com/Arize-ai/arize/issues/62005)) ([fb8200c](https://github.com/Arize-ai/arize/commit/fb8200c2bb0d14ae9878bfce73ac93f92b120fe1))
- convert bool values to strings in java sdk ([#61984](https://github.com/Arize-ai/arize/issues/61984)) ([ee881e3](https://github.com/Arize-ai/arize/commit/ee881e3088b3c979731f253ebf8798d019c0f4d4))

## [2.2.5](https://github.com/Arize-ai/arize/compare/arize-java-sdk/v2.2.4...arize-java-sdk/v2.2.5) (2026-02-04)

### 📚 Documentation

- add missing newline in headings ([#62001](https://github.com/Arize-ai/arize/issues/62001)) ([8622729](https://github.com/Arize-ai/arize/commit/86227297c4489bf3eb766aee0f97afea7b30ef32))

## [2.2.4](https://github.com/Arize-ai/arize/compare/arize-java-sdk/v2.2.3...arize-java-sdk/v2.2.4) (2026-02-04)

### 📚 Documentation

- add missing newline before sign-up step in guide ([#61997](https://github.com/Arize-ai/arize/issues/61997)) ([ceedcea](https://github.com/Arize-ai/arize/commit/ceedcea193db04033e52d98e010e430eb8878fad))

## [2.2.3](https://github.com/Arize-ai/arize/compare/arize-java-sdk/v2.2.2...arize-java-sdk/v2.2.3) (2026-02-04)

### 📚 Documentation

- improve README formatting for better readability ([#61993](https://github.com/Arize-ai/arize/issues/61993)) ([dc85ee9](https://github.com/Arize-ai/arize/commit/dc85ee98cbd696eefc87c4543d8e6b7703922f04))

## [2.2.2](https://github.com/Arize-ai/arize/compare/arize-java-sdk/v2.2.1...arize-java-sdk/v2.2.2) (2026-02-04)

### 🐛 Bug Fixes

- removes deprecated `Label` field on the Record in java SDK ([#60960](https://github.com/Arize-ai/arize/issues/60960)) ([fc8cdfc](https://github.com/Arize-ai/arize/commit/fc8cdfc71e0896f089e2547ba1d586b8747bfaf8))

### 📚 Documentation

- add README Overview section and clarify API key retrieval steps ([#61987](https://github.com/Arize-ai/arize/issues/61987)) ([87b667f](https://github.com/Arize-ai/arize/commit/87b667f12a409dcdc76e6d17ec6939554e28c8c1))
- parameterize Arize API client version and SHA1 in Maven and Bazel configs ([#61944](https://github.com/Arize-ai/arize/issues/61944)) ([5901bb1](https://github.com/Arize-ai/arize/commit/5901bb1e72eaed91a6ae60f92a77a42c8f80b9a0))

### ❔ Miscellaneous Chores

- **java-sdk:** update Maven Central publishing configuration ([#61559](https://github.com/Arize-ai/arize/issues/61559)) ([9c9586c](https://github.com/Arize-ai/arize/commit/9c9586c9530adff17b653206150ddcffb5efa757))
- **release:** automate changelog generation and include CHANGELOG.md in release config ([#61956](https://github.com/Arize-ai/arize/issues/61956)) ([4d02735](https://github.com/Arize-ai/arize/commit/4d027351b4ec2771ca7213fad0c5f120748842d6))
- **release:** enable Java API client release and Copybara integration ([#61650](https://github.com/Arize-ai/arize/issues/61650)) ([3f05b46](https://github.com/Arize-ai/arize/commit/3f05b4653a2985e3d4489e7948b35cd29ca541df))

## [2.2.1](https://github.com/Arize-ai/arize/tree/arize-java-sdk/v2.2.1) (2026-01-26)

Initial release with automated changelog generation via release-please. Previous releases were managed manually.
