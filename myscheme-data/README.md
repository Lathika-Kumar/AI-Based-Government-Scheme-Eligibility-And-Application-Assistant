# myScheme Data Extraction & Inspection Hub

## Overview
This directory contains the automated scripts, raw API datasets, normalized models, and audit logs for the official **myScheme** government scheme database extraction.

## Directory Structure
```text
myscheme-data/
├── raw/
│   ├── listing-page-000.json
│   ├── ...
│   ├── all-schemes-list.json
│   ├── details/
│   │   ├── kcc.json
│   │   ├── sui.json
│   │   └── ...
│   └── details-manifest.json
├── normalized/
│   ├── kcc-normalized.json
│   ├── sui-normalized.json
│   └── sample-schemes-normalized.json
├── scripts/
│   ├── find_key.py
│   ├── extract_scheme_list.py
│   ├── extract_scheme_details.py
│   ├── analyze_dataset.py
│   └── normalize_samples.py
├── logs/
│   ├── extraction_listing.log
│   └── extraction_details.log
├── DATA_AUDIT.md
└── README.md
```

## Security & API Key
- API access uses the `x-api-key` header via environment variable `MYSCHEME_API_KEY`.
- API keys are resolveable at runtime and are **never** hardcoded or committed to source repositories or logs.

## Commands

### 1. Extract Scheme Listing
```bash
python myscheme-data/scripts/extract_scheme_list.py
```

### 2. Analyze Extracted Dataset
```bash
python myscheme-data/scripts/analyze_dataset.py
```

### 3. Extract Bulk Scheme Details
```bash
python myscheme-data/scripts/extract_scheme_details.py
```

### 4. Generate Normalized Scheme Samples
```bash
python myscheme-data/scripts/normalize_samples.py
```
