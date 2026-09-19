#!/usr/bin/env python3
"""
SchemeBridge Standalone Random Forest Model Evaluation & Inference Script
File: ml/random_forest/evaluate_model.py

Purpose:
Loads the trained serialized model from ml/random_forest/models/random_forest_scheme_model.pkl,
validates its pipeline components, and performs evaluation and inference on citizen profiles.
"""

import os
import sys
import json
import time
import joblib
import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix


def evaluate():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(base_dir, "models", "random_forest_scheme_model.pkl")
    data_path = os.path.join(base_dir, "data", "synthetic_citizen_eligibility_dataset.csv")

    if not os.path.exists(model_path):
        print(f"[!] Error: Model file not found at {model_path}.")
        print("[!] Please run train_random_forest.py first.")
        sys.exit(1)

    if not os.path.exists(data_path):
        print(f"[!] Error: Dataset file not found at {data_path}.")
        print("[!] Please run train_random_forest.py first.")
        sys.exit(1)

    print("==================================================")
    print("SCHEMEBRIDGE RANDOM FOREST MODEL EVALUATION")
    print("==================================================")
    print(f"Loading model from: {model_path}")
    checkpoint = joblib.load(model_path)
    pipeline = checkpoint["pipeline"]
    trained_at = checkpoint.get("trained_at", "Unknown")
    print(f"Model trained at: {trained_at}")
    print(f"Pipeline steps: {[step[0] for step in pipeline.steps]}")
    print("")

    df = pd.read_csv(data_path)
    target_col = "eligible"
    X = df.drop(columns=[target_col])
    y = df[target_col]

    # Measure inference latency on the full 5,000 samples
    t0 = time.perf_counter()
    y_pred = pipeline.predict(X)
    t1 = time.perf_counter()
    total_time_ms = (t1 - t0) * 1000.0
    latency_per_sample_us = (total_time_ms / len(X)) * 1000.0

    acc = accuracy_score(y, y_pred)
    print("Evaluation on Complete Dataset:")
    print(f"Total samples: {len(X)}")
    print(f"Overall Accuracy: {acc:.4f} ({acc*100:.2f}%)")
    print(f"Inference Latency: {total_time_ms:.2f} ms total ({latency_per_sample_us:.2f} us/sample)")
    print("")

    # Demonstrate sample profile inference
    sample_profiles = pd.DataFrame([
        {
            "age": 42,
            "gender": "MALE",
            "state": "TAMIL_NADU",
            "annual_income": 120000.0,
            "occupation": "FARMER",
            "category": "OBC",
            "marital_status": "MARRIED",
            "disability_status": 0,
            "rural_urban_status": "RURAL",
            "employment_status": "SELF_EMPLOYED",
            "land_ownership": 2.5,
            "bpl_status": 1,
        },
        {
            "age": 35,
            "gender": "MALE",
            "state": "MAHARASHTRA",
            "annual_income": 850000.0,
            "occupation": "SALARIED",
            "category": "GENERAL",
            "marital_status": "SINGLE",
            "disability_status": 0,
            "rural_urban_status": "URBAN",
            "employment_status": "EMPLOYED",
            "land_ownership": 0.0,
            "bpl_status": 0,
        },
        {
            "age": 22,
            "gender": "FEMALE",
            "state": "KARNATAKA",
            "annual_income": 80000.0,
            "occupation": "STUDENT",
            "category": "SC",
            "marital_status": "SINGLE",
            "disability_status": 0,
            "rural_urban_status": "RURAL",
            "employment_status": "STUDENT",
            "land_ownership": 0.0,
            "bpl_status": 1,
        }
    ])

    print("Sample Citizen Inferences:")
    probs = pipeline.predict_proba(sample_profiles)
    preds = pipeline.predict(sample_profiles)

    descriptions = [
        "Farmer (Tamil Nadu, 2.5 acres, 1.2L income, BPL)",
        "Urban Salaried (Maharashtra, 8.5L income, General)",
        "Student (Karnataka, SC, 80k income, BPL)"
    ]

    for i, desc in enumerate(descriptions):
        label = "Eligible (1)" if preds[i] == 1 else "Not Eligible (0)"
        prob_elig = probs[i][1] * 100.0
        print(f"Profile {i+1}: {desc}")
        print(f"  -> Prediction: {label} (Probability Eligible: {prob_elig:.1f}%)")

    print("")
    print("==================================================")
    print("EVALUATION COMPLETE")
    print("==================================================")


if __name__ == "__main__":
    evaluate()
