# PinIn Changelog

## Unreleased

### Added
- add extra unicode data from supplementary panes [#12](https://github.com/ghostflyby/PinIn/pull/12)

### Changed

### Deprecated

### Removed

### Fixed

### Security

## 1.7.1 - 2025-09-30

### Added

- Support for matching characters in the Unicode Supplementary Planes.

## 1.4.0

### Changed

- Simplified format API for easier usage.

## 1.3.1

### Fixed

- `DictLoader` accessibility issue.

## 1.3.0

### Added

- Support for customized dictionary loading.

### Changed

- Improved `CachedSearcher` math model for massive data.

### Fixed

- Incorrect spelling in Phonetic keyboard starting with `v`.

## 1.2.0

### Changed

- Match rules:
    - Tone is no longer acceptable when missing vowel.  
      e.g. 「测试文本」 no longer matches `c4shi`, but still matches `ce4shi`.
    - Pinyin sequence (音序) can be used as abbreviation in Quanpin (全拼).  
      e.g. 「测试文本」 now matches `cswb` without fuzzy option,  
      while in previous versions it only matched `cshwb`.
- Some APIs updated.

### Added

- Accelerator switch (disabled by default):
    - When calling immediate matching functions continuously with different `s1` but same `s2` (e.g., 100 times), they
      are considered stable calls.
    - If most `s1` contains Chinese characters and the scenario is mainly stable calls, accelerator provides significant
      speed up.
    - Otherwise, the overhead slows down the process.
    - Disabled by default to keep consistent performance across scenarios.

### Fixed

- Several spelling issues in Daqian (大千) keyboard.
