# SchemeBridge — Standalone Random Forest ML Module
## Citizen Welfare Scheme Recommendation Model

> **IMPORTANT ARCHITECTURAL STATEMENT:**  
> **"The Random Forest module is a standalone ML demonstration of citizen welfare scheme recommendation and is not connected to the current production application flow."**  
>  
> The core SchemeBridge application uses a deterministic Boolean AST statutory rules engine (`EligibilityEngine.java`) combined with off-heap dense semantic vector retrieval (`SemanticEmbeddingIndexService.java`). This module exists strictly as an independent, reproducible machine learning demonstration and research pipeline.

---

## 1. Purpose

The purpose of this model is defined as:
**"Citizen Welfare Scheme Recommendation Model"**

The model demonstrates how citizen profile attributes can be used by a scikit-learn `RandomForestClassifier` to recommend the most relevant government welfare scheme category for a citizen based on their demographic, economic, and geographic profile.

### Conceptual Workflow
```
Citizen Profile
      ↓
Feature Processing (StandardScaler + OneHotEncoder)
      ↓
Random Forest Classifier (200 Decision Trees)
      ↓
Predicted Scheme Category
      ↓
Recommended Welfare Scheme
```

---

## 2. Problem Statement

Citizens seeking government welfare schemes often struggle to identify which category of public welfare assistance applies to their specific life circumstances. While statutory legal eligibility requires strict deterministic rule verification, machine learning classifiers can rapidly assess multidimensional citizen attributes (such as low income, agrarian occupation, student status, disability, or elder age) and categorize the citizen into the optimal welfare support program.

---

## 3. Dataset

- **Dataset File:** `ml/random_forest/data/synthetic_citizen_eligibility_dataset.csv`
- **Total Records:** 5,000 citizen profiles
- **Data Label:** **"Demonstration / synthetic ML dataset for Random Forest scheme recommendation."**
- **Ground Truth Methodology:** The dataset models realistic Indian citizen demographics mapped against multi-criteria statutory welfare gatekeepers, incorporating realistic socioeconomic distributions and a calibrated 1.5% edge-case stochastic variation.

---

## 4. Features Used

The model uses 9 citizen profile attributes:

| Feature Name | Type | Description | Values / Examples |
|---|---|---|---|
| `age` | Numerical | Citizen age in years | 18 – 75 |
| `gender` | Categorical | Legal gender | `Female`, `Male`, `Transgender` |
| `state` | Categorical | State of domicile | `Tamil Nadu`, `Maharashtra`, `Uttar Pradesh`, `Karnataka`, `Kerala`, `Bihar`, `Rajasthan`, `Gujarat`, `Madhya Pradesh`, `West Bengal` |
| `income` | Numerical | Annual household income in INR | 30,000 – 1,200,000+ |
| `occupation` | Categorical | Primary occupation | `Student`, `Farmer`, `Artisan`, `Self-Employed`, `Daily Wage Worker`, `Unemployed`, `Salaried` |
| `category` | Categorical | Social reservation category | `OBC`, `General`, `SC`, `ST`, `EWS` |
| `rural` | Categorical | Rural residence indicator | `Yes`, `No` |
| `student` | Categorical | Enrolled student indicator | `Yes`, `No` |
| `disability` | Categorical | Person with disability indicator | `Yes`, `No` |

---

## 5. Target Variable

- **Target Column:** `scheme_category`
- **Problem Type:** Multi-class classification (7 discrete scheme categories)
- **Target Categories:**
  1. `Agriculture & Farmer Welfare` — Income support, crop protection, equipment subsidies, Krishi assistance
  2. `Education & Scholarships` — Pre/post-matric scholarships, tuition fee waivers, merit-cum-means grants
  3. `Women & Child Development` — Maternal benefits, girl child education, women entrepreneurship, self-help groups
  4. `Social Welfare & Disability Support` — Assistive equipment, disability pensions, affirmative action grants
  5. `Senior Citizen Pension & Healthcare` — Old-age social security pensions, geriatric healthcare
  6. `Skill Development & Entrepreneurship` — Mudra loans, PMEGP, artisan toolkits, vocational skilling
  7. `Rural & Urban Livelihood Support` — MGNREGA, PM Awas Yojana housing, BPL family livelihood aid

---

## 6. Data Preprocessing

Features are processed via a scikit-learn `ColumnTransformer` inside an end-to-end `Pipeline`:
1. **Numerical Features (`age`, `income`):** Scaled using `StandardScaler` to normalize zero mean and unit variance.
2. **Categorical Features (`gender`, `state`, `occupation`, `category`, `rural`, `student`, `disability`):** Encoded using `OneHotEncoder(handle_unknown='ignore', sparse_output=False)`.
3. **Data Splitting:** 80% Training (4,000 samples) and 20% Testing (1,000 samples) using stratified sampling based on the target variable (`random_state=42`).

---

## 7. Random Forest Algorithm

The model uses `sklearn.ensemble.RandomForestClassifier` configured with:
- `n_estimators`: 200 trees
- `max_depth`: 16
- `min_samples_split`: 4
- `min_samples_leaf`: 2
- `class_weight`: `"balanced"` (to prevent majority category bias)
- `random_state`: 42
- `n_jobs`: -1 (parallel CPU tree construction)

---

## 8. Training Workflow

Execute the training script:
```bash
python ml/random_forest/train_random_forest.py
```

The script performs:
1. Loading/generating the demonstration dataset.
2. Preprocessing numerical and categorical features.
3. Stratified 80/20 train/test splitting.
4. Training the 200-estimator `RandomForestClassifier`.
5. Predicting on held-out test data.
6. Calculating accuracy, weighted precision, recall, and F1 score.
7. Generating the confusion matrix plot (`results/confusion_matrix.png`).
8. Ranking feature importances (`results/feature_importance.csv`).
9. Serializing the trained pipeline to `models/random_forest_scheme_model.pkl`.
10. Saving detailed evaluation metrics to `results/metrics.json`.

---

## 9. Evaluation Metrics

Evaluated on the held-out test split (1,000 test samples):

| Metric | Measured Value |
|---|---|
| **Accuracy** | **98.40%** (0.9840) |
| **Weighted Precision** | **98.42%** (0.9842) |
| **Weighted Recall** | **98.40%** (0.9840) |
| **Weighted F1 Score** | **98.40%** (0.9840) |
| **Inference Latency** | ~21.6 µs / sample (~108 ms for 5,000 samples) |

---

## 10. Sample Prediction

Run the evaluation and inference demonstration:
```bash
python ml/random_forest/evaluate_model.py
```

### Demonstration Output

```
==================================================
SAMPLE PREDICTION (STANDALONE RANDOM FOREST)
==================================================
Input Citizen Profile:

Age: 22
Gender: Female
State: Tamil Nadu
Income: 120000
Occupation: Student
Category: OBC
Rural: Yes
Student: Yes
Disability: No

Predicted Scheme Category:
Education & Scholarships
(Confidence / Model Probability: 97.0%)
==================================================
```

### Additional Profiles Tested

- **Rural Farmer** (Age 45, Male, Maharashtra, Farmer, Income 1.5L) $\rightarrow$ `Agriculture & Farmer Welfare` (94.1% confidence)
- **Elderly Citizen** (Age 67, Male, Uttar Pradesh, Unemployed, Income 60k) $\rightarrow$ `Senior Citizen Pension & Healthcare` (83.7% confidence)
- **Urban Artisan** (Age 34, Female, Karnataka, Artisan, Income 95k) $\rightarrow$ `Women & Child Development` (96.4% confidence)
- **Person with Disability** (Age 29, Male, Kerala, Disability: Yes, Income 80k) $\rightarrow$ `Social Welfare & Disability Support` (93.6% confidence)

---

## 11. Model Artifacts

- **Trained Model Artifact:** `ml/random_forest/models/random_forest_scheme_model.pkl` (contains full scikit-learn Pipeline, encoders, scalers, and trained classifier)
- **Evaluation Metrics:** `ml/random_forest/results/metrics.json`
- **Confusion Matrix Plot:** `ml/random_forest/results/confusion_matrix.png`
- **Feature Importance:** `ml/random_forest/results/feature_importance.csv`
- **Classification Report:** `ml/random_forest/results/classification_report.txt`

---

## 12. Limitations

1. **Synthetic Training Data:** The model is trained on a synthetic demonstration dataset representing 5,000 citizen profiles. While statistically modeled after Indian welfare rules, it does not represent live administrative enrollment records.
2. **Category-Level Output:** The model predicts a high-level *scheme category* rather than individual scheme statutory codes (e.g. predicting `Agriculture & Farmer Welfare` rather than resolving between PM-KISAN vs. State Rythu Bandhu).
3. **No Statutory Authority:** In government welfare delivery, deterministic legal eligibility (e.g. land ownership thresholds, caste certification) cannot be replaced by statistical probability. SchemeBridge deliberately keeps statutory legal eligibility strictly deterministic in Java (`EligibilityEngine.java`).
