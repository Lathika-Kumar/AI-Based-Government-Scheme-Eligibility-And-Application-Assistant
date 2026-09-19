#!/usr/bin/env python3
"""
SchemeBridge Standalone Random Forest Machine Learning Experimentation Pipeline
File: ml/random_forest/train_random_forest.py

Purpose:
Trains and evaluates a scikit-learn Pipeline with ColumnTransformer, OneHotEncoder,
and RandomForestClassifier(n_estimators=200, random_state=42, class_weight='balanced')
for citizen welfare scheme eligibility classification.

Architectural Isolation:
- This module is strictly an offline experimentation pipeline.
- It is NOT connected to or referenced by Spring Boot backend, DocumentExtractionService,
  EligibilityEngine, or React frontend.
- Zero modifications to production databases or services.
"""

import os
import sys
import json
from datetime import datetime, timezone

import numpy as np
import pandas as pd
import joblib

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix,
)


def get_paths():
    """Returns directory and file paths relative to this script."""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(base_dir, "data")
    models_dir = os.path.join(base_dir, "models")
    results_dir = os.path.join(base_dir, "results")

    os.makedirs(data_dir, exist_ok=True)
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(results_dir, exist_ok=True)

    return {
        "base_dir": base_dir,
        "data_path": os.path.join(data_dir, "synthetic_citizen_eligibility_dataset.csv"),
        "model_path": os.path.join(models_dir, "random_forest_scheme_model.pkl"),
        "metrics_path": os.path.join(results_dir, "metrics.json"),
        "report_path": os.path.join(results_dir, "classification_report.txt"),
        "cm_path": os.path.join(results_dir, "confusion_matrix.png"),
        "importance_path": os.path.join(results_dir, "feature_importance.csv"),
    }


def generate_or_load_dataset(csv_path: str) -> pd.DataFrame:
    """
    Loads or creates the documented experimental citizen eligibility dataset.
    Note on Dataset Origin & Ground Truth:
    The raw SchemeBridge scheme catalog (master_schemes_4734.json) contains official
    scheme criteria definitions, but does not contain labeled citizen application
    decisions. To perform supervised Random Forest training, this dataset models
    citizen demographic profiles (strictly mirroring CitizenEligibilityProfile.java)
    evaluated against statutory multi-criteria welfare gatekeepers.
    """
    if os.path.exists(csv_path):
        return pd.read_csv(csv_path)

    print(f"[*] Generating reproducible experimental dataset at: {csv_path}")
    np.random.seed(42)
    n_samples = 5000

    ages = np.random.randint(18, 76, size=n_samples)
    genders = np.random.choice(["MALE", "FEMALE", "TRANSGENDER"], size=n_samples, p=[0.50, 0.48, 0.02])
    states = np.random.choice([
        "TAMIL_NADU", "MAHARASHTRA", "UTTAR_PRADESH", "KARNATAKA",
        "KERALA", "BIHAR", "RAJASTHAN", "GUJARAT", "MADHYA_PRADESH", "WEST_BENGAL"
    ], size=n_samples)
    incomes = np.round(np.random.exponential(scale=200000, size=n_samples) + 25000, -2)
    occupations = np.random.choice([
        "FARMER", "STUDENT", "SELF_EMPLOYED", "UNEMPLOYED", "SALARIED", "ARTISAN", "DAILY_WAGE_WORKER"
    ], size=n_samples, p=[0.24, 0.16, 0.18, 0.12, 0.14, 0.08, 0.08])
    categories = np.random.choice(["GENERAL", "OBC", "SC", "ST", "EWS"], size=n_samples, p=[0.28, 0.38, 0.18, 0.10, 0.06])
    marital = np.random.choice(["SINGLE", "MARRIED", "WIDOWED", "DIVORCED"], size=n_samples, p=[0.25, 0.62, 0.09, 0.04])
    disability = np.random.choice([0, 1], size=n_samples, p=[0.94, 0.06])
    rural_urban = np.random.choice(["RURAL", "URBAN", "SEMI_URBAN"], size=n_samples, p=[0.60, 0.25, 0.15])
    employment = np.random.choice(["EMPLOYED", "UNEMPLOYED", "SELF_EMPLOYED", "STUDENT"], size=n_samples, p=[0.45, 0.15, 0.25, 0.15])
    land = np.where(occupations == "FARMER", np.round(np.random.uniform(0.5, 10.0, size=n_samples), 1), 0.0)
    bpl = np.where(incomes <= 120000, np.random.choice([0, 1], size=n_samples, p=[0.15, 0.85]), 0)

    # Multi-criteria statutory eligibility evaluation rules:
    eligible = []
    for i in range(n_samples):
        # Criterion 1: Small/Marginal Farmer Income Support (e.g., PM-KISAN, State Rythu/Krishi schemes)
        c1 = (occupations[i] == "FARMER") and (land[i] <= 5.0) and (incomes[i] <= 400000)
        # Criterion 2: Low Income / BPL / Affirmative Action Welfare (e.g., PMAY, Ayushman, State Aid)
        c2 = (bpl[i] == 1 or incomes[i] <= 250000) and (disability[i] == 1 or categories[i] in ["SC", "ST", "EWS", "OBC"]) and (ages[i] >= 18)
        # Criterion 3: Youth / Student Education & Scholarship Grant (e.g., NSP, Post-Matric)
        c3 = (occupations[i] == "STUDENT") and (ages[i] <= 28) and (incomes[i] <= 300000)
        # Criterion 4: Women Economic Empowerment & Pension Support
        c4 = (genders[i] == "FEMALE") and (incomes[i] <= 350000) and (marital[i] in ["WIDOWED", "DIVORCED", "MARRIED"]) and (ages[i] >= 21)

        is_elig = 1 if (c1 or c2 or c3 or c4) else 0
        eligible.append(is_elig)

    df = pd.DataFrame({
        "age": ages,
        "gender": genders,
        "state": states,
        "annual_income": incomes,
        "occupation": occupations,
        "category": categories,
        "marital_status": marital,
        "disability_status": disability,
        "rural_urban_status": rural_urban,
        "employment_status": employment,
        "land_ownership": land,
        "bpl_status": bpl,
        "eligible": eligible,
    })

    df.to_csv(csv_path, index=False)
    return df


def plot_and_save_confusion_matrix(cm, save_path: str):
    """Generates and saves an annotated confusion matrix heatmap."""
    fig, ax = plt.subplots(figsize=(6, 5), dpi=300)
    im = ax.imshow(cm, interpolation="nearest", cmap=plt.cm.Blues)
    ax.figure.colorbar(im, ax=ax, fraction=0.046, pad=0.04)

    classes = ["Not Eligible (0)", "Eligible (1)"]
    ax.set(
        xticks=np.arange(cm.shape[1]),
        yticks=np.arange(cm.shape[0]),
        xticklabels=classes,
        yticklabels=classes,
        title="Random Forest Confusion Matrix\nCitizen Welfare Scheme Eligibility",
        ylabel="True Label",
        xlabel="Predicted Label",
    )

    thresh = cm.max() / 2.0
    for i in range(cm.shape[0]):
        for j in range(cm.shape[1]):
            ax.text(
                j, i, format(cm[i, j], "d"),
                ha="center", va="center",
                color="white" if cm[i, j] > thresh else "black",
                fontsize=11, fontweight="bold"
            )

    fig.tight_layout()
    plt.savefig(save_path, bbox_inches="tight")
    plt.close(fig)


def train_model():
    """Main training execution."""
    paths = get_paths()

    # 1. Load Dataset
    df = generate_or_load_dataset(paths["data_path"])
    target_col = "eligible"

    X = df.drop(columns=[target_col])
    y = df[target_col]

    num_samples = len(df)
    num_features = X.shape[1]
    class_dist = y.value_counts().to_dict()

    # Pre-training dataset summary
    print("==================================================")
    print("RANDOM FOREST TRAINING")
    print("==================================================")
    print(f"Dataset shape: {num_samples} x {num_features}")
    print(f"Number of samples: {num_samples}")
    print(f"Number of features: {num_features}")
    print(f"Target column: {target_col}")
    print("Class distribution:")
    for k in sorted(class_dist.keys()):
        print(f"{k} -> {class_dist[k]}")
    print("==================================================")
    print("")

    # 2. Define Features & ColumnTransformer Preprocessor
    cat_cols = ["gender", "state", "occupation", "category", "marital_status", "rural_urban_status", "employment_status"]
    num_cols = ["age", "annual_income", "disability_status", "land_ownership", "bpl_status"]

    preprocessor = ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), num_cols),
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), cat_cols),
        ]
    )

    # 3. Define Pipeline with RandomForestClassifier
    n_estimators = 200
    random_state = 42
    class_weight = "balanced"

    rf_classifier = RandomForestClassifier(
        n_estimators=n_estimators,
        random_state=random_state,
        class_weight=class_weight,
        n_jobs=-1,
    )

    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", rf_classifier),
        ]
    )

    # 4. Stratified Train / Test Split
    test_size = 0.20
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=random_state, stratify=y
    )

    train_samples = len(X_train)
    test_samples = len(X_test)

    # 5. Train Model
    pipeline.fit(X_train, y_train)

    # 6. Evaluate Model on Holdout Test Set
    y_pred = pipeline.predict(X_test)

    acc = float(accuracy_score(y_test, y_pred))
    prec = float(precision_score(y_test, y_pred, average="weighted", zero_division=0))
    rec = float(recall_score(y_test, y_pred, average="weighted", zero_division=0))
    f1 = float(f1_score(y_test, y_pred, average="weighted", zero_division=0))

    class_rep_text = classification_report(y_test, y_pred, digits=4, zero_division=0)
    class_rep_dict = classification_report(y_test, y_pred, output_dict=True, zero_division=0)
    cm = confusion_matrix(y_test, y_pred)

    # 7. Extract Feature Importances
    preprocessor_fitted = pipeline.named_steps["preprocessor"]
    rf_fitted = pipeline.named_steps["classifier"]
    encoded_feature_names = preprocessor_fitted.get_feature_names_out()

    importances = pd.DataFrame({
        "feature": encoded_feature_names,
        "importance": rf_fitted.feature_importances_,
    }).sort_values(by="importance", ascending=False)
    importances.to_csv(paths["importance_path"], index=False)

    # 8. Save Confusion Matrix and Classification Report
    plot_and_save_confusion_matrix(cm, paths["cm_path"])
    with open(paths["report_path"], "w", encoding="utf-8") as f:
        f.write(class_rep_text)

    # 9. Save Serialized Model
    joblib.dump({
        "pipeline": pipeline,
        "feature_columns": list(X.columns),
        "target_column": target_col,
        "encoded_feature_names": list(encoded_feature_names),
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "n_samples": num_samples,
    }, paths["model_path"])

    # 10. Save Metrics JSON
    metrics_payload = {
        "modelName": "RandomForestClassifier",
        "algorithm": "Random Forest",
        "framework": "scikit-learn",
        "trainedAt": datetime.now(timezone.utc).isoformat(),
        "hyperparameters": {
            "n_estimators": n_estimators,
            "random_state": random_state,
            "class_weight": class_weight,
            "test_size": test_size,
            "stratify": True,
        },
        "dataset": {
            "path": os.path.relpath(paths["data_path"], paths["base_dir"]),
            "type": "Synthetic Citizen Eligibility Demographics (Schema-aligned with CitizenEligibilityProfile.java)",
            "samples": num_samples,
            "features": num_features,
            "trainingSamples": train_samples,
            "testingSamples": test_samples,
            "targetVariable": target_col,
            "classes": {str(k): int(v) for k, v in class_dist.items()},
        },
        "performance": {
            "accuracy": round(acc, 4),
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1Score": round(f1, 4),
            "classificationReport": class_rep_dict,
            "confusionMatrix": cm.tolist(),
        },
        "artifacts": {
            "model": os.path.relpath(paths["model_path"], paths["base_dir"]),
            "metrics": os.path.relpath(paths["metrics_path"], paths["base_dir"]),
            "report": os.path.relpath(paths["report_path"], paths["base_dir"]),
            "confusionMatrix": os.path.relpath(paths["cm_path"], paths["base_dir"]),
            "featureImportance": os.path.relpath(paths["importance_path"], paths["base_dir"]),
        },
        "governance": {
            "isolatedFromProduction": True,
            "liveServiceReplaced": False,
            "statutoryEligibilityOverridden": False,
            "notes": "Standalone ML experimentation module for academic research and model evaluation."
        }
    }

    with open(paths["metrics_path"], "w", encoding="utf-8") as f:
        json.dump(metrics_payload, f, indent=2)

    # 11. Print Section 7 Terminal Output
    print("==================================================")
    print("SCHEMEBRIDGE RANDOM FOREST TRAINING")
    print("==================================================")
    print("")
    print("Dataset:")
    print(f"ml/random_forest/data/synthetic_citizen_eligibility_dataset.csv")
    print("")
    print("Samples:")
    print(f"{num_samples} (Train: {train_samples}, Test: {test_samples})")
    print("")
    print("Features:")
    print(f"{num_features} raw features ({len(encoded_feature_names)} encoded columns)")
    print("")
    print("Target:")
    print(f"{target_col} (Classes: {class_dist})")
    print("")
    print("--------------------------------------------------")
    print("MODEL")
    print("--------------------------------------------------")
    print("Algorithm: Random Forest Classifier")
    print(f"Number of Trees: {n_estimators}")
    print(f"Random State: {random_state}")
    print(f"Class Weight: {class_weight}")
    print("")
    print("--------------------------------------------------")
    print("TRAINING")
    print("--------------------------------------------------")
    print(f"Training samples: {train_samples}")
    print(f"Testing samples: {test_samples}")
    print("")
    print("Training completed successfully.")
    print("")
    print("--------------------------------------------------")
    print("PERFORMANCE")
    print("--------------------------------------------------")
    print(f"Accuracy : {acc:.4f}")
    print(f"Precision: {prec:.4f}")
    print(f"Recall   : {rec:.4f}")
    print(f"F1 Score : {f1:.4f}")
    print("")
    print("--------------------------------------------------")
    print("CLASSIFICATION REPORT")
    print("--------------------------------------------------")
    print(class_rep_text)
    print("--------------------------------------------------")
    print("CONFUSION MATRIX")
    print("--------------------------------------------------")
    print(cm)
    print("")
    print("--------------------------------------------------")
    print("FEATURE IMPORTANCE")
    print("--------------------------------------------------")
    for idx, row in importances.head(10).iterrows():
        print(f"  {row['feature']:<35} : {row['importance']:.4f}")
    print("")
    print("Model saved to:")
    print(f"ml/random_forest/models/random_forest_scheme_model.pkl")
    print("")
    print("Metrics saved to:")
    print(f"ml/random_forest/results/metrics.json")
    print("")
    print("==================================================")
    print("TRAINING COMPLETE")
    print("==================================================")


if __name__ == "__main__":
    train_model()
