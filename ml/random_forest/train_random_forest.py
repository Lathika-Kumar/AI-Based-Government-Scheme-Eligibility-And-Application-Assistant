#!/usr/bin/env python3
"""
SchemeBridge Standalone Random Forest Machine Learning Experimentation Pipeline
File: ml/random_forest/train_random_forest.py

Purpose:
"Citizen Welfare Scheme Recommendation Model"
The model demonstrates how citizen profile attributes can be used by a Random Forest
classifier to recommend the most relevant government welfare scheme category for a citizen.

Conceptual Output Flow:
Citizen Profile -> Feature Processing -> Random Forest Classifier -> Predicted Scheme Category -> Recommended Welfare Scheme

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


def generate_or_load_dataset(csv_path: str, force_regenerate: bool = False) -> pd.DataFrame:
    """
    Loads or creates the documented demonstration dataset.
    
    Label: Demonstration / synthetic ML dataset for Random Forest scheme recommendation.
    Note:
    This dataset models citizen demographic profiles and maps them to appropriate
    statutory welfare scheme categories. It is strictly for standalone ML demonstration.
    """
    if os.path.exists(csv_path) and not force_regenerate:
        # Check if existing dataset has scheme_category
        existing_df = pd.read_csv(csv_path)
        if "scheme_category" in existing_df.columns:
            return existing_df

    print(f"[*] Generating reproducible demonstration dataset at: {csv_path}")
    np.random.seed(42)
    n_samples = 5000

    # Attributes
    ages = np.random.randint(18, 76, size=n_samples)
    genders = np.random.choice(["Female", "Male", "Transgender"], size=n_samples, p=[0.49, 0.49, 0.02])
    states = np.random.choice([
        "Tamil Nadu", "Maharashtra", "Uttar Pradesh", "Karnataka",
        "Kerala", "Bihar", "Rajasthan", "Gujarat", "Madhya Pradesh", "West Bengal"
    ], size=n_samples)
    
    # Incomes in INR: distributed across economically weaker, lower-middle, and middle class
    incomes = np.round(np.random.exponential(scale=180000, size=n_samples) + 30000, -2)
    
    occupations = np.random.choice([
        "Student", "Farmer", "Artisan", "Self-Employed", "Daily Wage Worker", "Unemployed", "Salaried"
    ], size=n_samples, p=[0.18, 0.22, 0.10, 0.16, 0.12, 0.10, 0.12])
    
    categories = np.random.choice(["OBC", "General", "SC", "ST", "EWS"], size=n_samples, p=[0.38, 0.28, 0.18, 0.10, 0.06])
    rural_flags = np.random.choice(["Yes", "No"], size=n_samples, p=[0.62, 0.38])
    
    # Students: students or youth under 25 who study
    student_flags = []
    for i in range(n_samples):
        if occupations[i] == "Student":
            student_flags.append("Yes")
        elif ages[i] <= 24 and np.random.rand() < 0.15:
            student_flags.append("Yes")
        else:
            student_flags.append("No")
    student_flags = np.array(student_flags)

    # Disability status: ~6% of citizens
    disability_flags = np.random.choice(["Yes", "No"], size=n_samples, p=[0.06, 0.94])

    # Target: scheme_category
    # Categories:
    # 1. "Education & Scholarships"
    # 2. "Agriculture & Farmer Welfare"
    # 3. "Social Welfare & Disability Support"
    # 4. "Senior Citizen Pension & Healthcare"
    # 5. "Women & Child Development"
    # 6. "Skill Development & Entrepreneurship"
    # 7. "Rural & Urban Livelihood Support"
    scheme_categories = []
    for i in range(n_samples):
        age = ages[i]
        gender = genders[i]
        inc = incomes[i]
        occ = occupations[i]
        cat = categories[i]
        rur = rural_flags[i]
        stu = student_flags[i]
        dis = disability_flags[i]

        # Decision logic for statutory category assignment
        if dis == "Yes" and inc <= 500000:
            target = "Social Welfare & Disability Support"
        elif stu == "Yes" or occ == "Student":
            target = "Education & Scholarships"
        elif age >= 60:
            target = "Senior Citizen Pension & Healthcare"
        elif occ == "Farmer":
            target = "Agriculture & Farmer Welfare"
        elif gender == "Female" and (inc <= 350000 or cat in ["SC", "ST", "OBC"] or rur == "Yes"):
            target = "Women & Child Development"
        elif occ in ["Artisan", "Self-Employed"]:
            target = "Skill Development & Entrepreneurship"
        else:
            target = "Rural & Urban Livelihood Support"

        # Realistic stochastic variance (1.5% edge case / alternative scheme match)
        if np.random.rand() < 0.015:
            possible = [
                "Education & Scholarships", "Agriculture & Farmer Welfare",
                "Social Welfare & Disability Support", "Senior Citizen Pension & Healthcare",
                "Women & Child Development", "Skill Development & Entrepreneurship",
                "Rural & Urban Livelihood Support"
            ]
            target = np.random.choice(possible)

        scheme_categories.append(target)

    df = pd.DataFrame({
        "age": ages,
        "gender": genders,
        "state": states,
        "income": incomes,
        "occupation": occupations,
        "category": categories,
        "rural": rural_flags,
        "student": student_flags,
        "disability": disability_flags,
        "scheme_category": scheme_categories,
    })

    df.to_csv(csv_path, index=False)
    print(f"[+] Dataset created with {len(df)} records.")
    return df


def plot_and_save_confusion_matrix(cm, labels, save_path: str):
    """Generates and saves an annotated confusion matrix heatmap."""
    fig, ax = plt.subplots(figsize=(10, 8), dpi=300)
    im = ax.imshow(cm, interpolation="nearest", cmap=plt.cm.Blues)
    ax.figure.colorbar(im, ax=ax, fraction=0.046, pad=0.04)

    # Format ticks
    ax.set(
        xticks=np.arange(cm.shape[1]),
        yticks=np.arange(cm.shape[0]),
        xticklabels=labels,
        yticklabels=labels,
        title="SchemeBridge Random Forest - Confusion Matrix\nCitizen Welfare Scheme Recommendation",
        ylabel="True Scheme Category",
        xlabel="Predicted Scheme Category",
    )
    plt.setp(ax.get_xticklabels(), rotation=35, ha="right", rotation_mode="anchor")

    # Annotate text
    thresh = cm.max() / 2.0
    for i in range(cm.shape[0]):
        for j in range(cm.shape[1]):
            val = cm[i, j]
            ax.text(
                j,
                i,
                f"{val:,}",
                ha="center",
                va="center",
                color="white" if val > thresh else "black",
                fontsize=8,
                fontweight="bold" if i == j else "normal",
            )

    fig.tight_layout()
    plt.savefig(save_path, bbox_inches="tight")
    plt.close()
    print(f"[+] Confusion matrix plot saved: {save_path}")


def main():
    paths = get_paths()
    print("==================================================")
    print("SCHEMEBRIDGE RANDOM FOREST ML TRAINING PIPELINE")
    print("Citizen Welfare Scheme Recommendation Model")
    print("==================================================")

    # Step 1 & 2: Load and inspect dataset
    df = generate_or_load_dataset(paths["data_path"], force_regenerate=True)
    total_records = len(df)
    print(f"[+] Total dataset records: {total_records}")
    print(f"[+] Scheme category distribution:\n{df['scheme_category'].value_counts()}\n")

    # Feature and target segregation
    target_col = "scheme_category"
    feature_cols = [c for c in df.columns if c != target_col]
    X = df[feature_cols]
    y = df[target_col]

    categorical_cols = ["gender", "state", "occupation", "category", "rural", "student", "disability"]
    numerical_cols = ["age", "income"]

    # Step 3: Preprocessing pipeline
    preprocessor = ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), numerical_cols),
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), categorical_cols),
        ]
    )

    # Step 4: Split into training and testing data (80/20 stratified)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )
    train_count = len(X_train)
    test_count = len(X_test)
    print(f"[+] Split: {train_count} training samples, {test_count} testing samples (80/20 stratified).")

    # Step 5: Configure and train RandomForestClassifier
    rf = RandomForestClassifier(
        n_estimators=200,
        max_depth=16,
        min_samples_split=4,
        min_samples_leaf=2,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1,
    )

    pipeline = Pipeline(steps=[
        ("preprocessor", preprocessor),
        ("classifier", rf),
    ])

    print("[*] Training RandomForestClassifier (200 trees)...")
    pipeline.fit(X_train, y_train)
    print("[+] Model training complete.")

    # Step 6: Generate predictions
    y_pred = pipeline.predict(X_test)

    # Step 7: Calculate evaluation metrics
    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, average="weighted", zero_division=0)
    rec = recall_score(y_test, y_pred, average="weighted", zero_division=0)
    f1 = f1_score(y_test, y_pred, average="weighted", zero_division=0)

    classes = sorted(list(pipeline.classes_))
    cm = confusion_matrix(y_test, y_pred, labels=classes)
    clf_report = classification_report(y_test, y_pred, digits=4, zero_division=0)

    # Save classification report
    with open(paths["report_path"], "w", encoding="utf-8") as f:
        f.write("SchemeBridge Random Forest - Classification Report\n")
        f.write("Citizen Welfare Scheme Recommendation Model\n")
        f.write(f"Evaluated on {test_count} test samples\n")
        f.write(f"Generated at: {datetime.now(timezone.utc).isoformat()}\n\n")
        f.write(clf_report)

    # Save metrics JSON
    metrics_payload = {
        "model_name": "RandomForestClassifier",
        "task": "Citizen Welfare Scheme Recommendation",
        "total_records": total_records,
        "train_samples": train_count,
        "test_samples": test_count,
        "features": {
            "numerical": numerical_cols,
            "categorical": categorical_cols,
            "all": feature_cols,
        },
        "target_variable": target_col,
        "classes": classes,
        "metrics": {
            "accuracy": round(float(acc), 4),
            "precision_weighted": round(float(prec), 4),
            "recall_weighted": round(float(rec), 4),
            "f1_score_weighted": round(float(f1), 4),
            "accuracy_percent": f"{acc * 100:.2f}%",
            "precision_percent": f"{prec * 100:.2f}%",
            "recall_percent": f"{rec * 100:.2f}%",
            "f1_percent": f"{f1 * 100:.2f}%",
        },
        "hyperparameters": {
            "n_estimators": 200,
            "max_depth": 16,
            "min_samples_split": 4,
            "min_samples_leaf": 2,
            "class_weight": "balanced",
            "random_state": 42,
        },
        "confusion_matrix": cm.tolist(),
        "trained_at_utc": datetime.now(timezone.utc).isoformat(),
        "architectural_status": "STANDALONE_OFFLINE_EXPERIMENTATION_ONLY",
    }

    with open(paths["metrics_path"], "w", encoding="utf-8") as f:
        json.dump(metrics_payload, f, indent=2)

    # Save confusion matrix plot
    plot_and_save_confusion_matrix(cm, classes, paths["cm_path"])

    # Extract and save feature importances
    ohe_step = pipeline.named_steps["preprocessor"].named_transformers_["cat"]
    cat_feature_names = ohe_step.get_feature_names_out(categorical_cols).tolist()
    all_feature_names = numerical_cols + cat_feature_names
    importances = pipeline.named_steps["classifier"].feature_importances_

    fi_df = pd.DataFrame({
        "feature": all_feature_names,
        "importance": importances,
    }).sort_values(by="importance", ascending=False)
    fi_df.to_csv(paths["importance_path"], index=False)
    print(f"[+] Feature importances saved: {paths['importance_path']}")

    # Step 8: Save trained model artifact
    model_artifact = {
        "pipeline": pipeline,
        "model": rf,
        "feature_cols": feature_cols,
        "numerical_cols": numerical_cols,
        "categorical_cols": categorical_cols,
        "target_col": target_col,
        "classes": classes,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "metrics": {
            "accuracy": acc,
            "precision": prec,
            "recall": rec,
            "f1": f1,
        },
    }
    joblib.dump(model_artifact, paths["model_path"], compress=3)
    print(f"[+] Model artifact saved: {paths['model_path']}")

    # Terminal summary block exactly matching presentation requirements
    print("\n========================================")
    print("SCHEMEBRIDGE RANDOM FOREST MODEL")
    print("Citizen Welfare Scheme Recommendation")
    print("========================================")
    print(f"Dataset records: {total_records}")
    print(f"Training samples: {train_count}")
    print(f"Testing samples: {test_count}")
    print("")
    print("Model: RandomForestClassifier")
    print("")
    print(f"Accuracy : {acc * 100:.2f}%")
    print(f"Precision: {prec * 100:.2f}%")
    print(f"Recall   : {rec * 100:.2f}%")
    print(f"F1 Score : {f1 * 100:.2f}%")
    print("")
    print("Model saved:")
    print("ml/random_forest/models/random_forest_scheme_model.pkl")
    print("")
    print("Evaluation results saved:")
    print("ml/random_forest/results/metrics.json")
    print("========================================\n")


if __name__ == "__main__":
    main()
