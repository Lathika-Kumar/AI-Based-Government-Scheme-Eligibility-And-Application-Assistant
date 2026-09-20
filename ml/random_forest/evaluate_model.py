#!/usr/bin/env python3
"""
SchemeBridge Standalone Random Forest Model Evaluation & Inference Script
File: ml/random_forest/evaluate_model.py

Purpose:
"Citizen Welfare Scheme Recommendation Model"
Loads the trained serialized model from ml/random_forest/models/random_forest_scheme_model.pkl,
validates its pipeline components, and performs evaluation and sample inference on citizen profiles.

Architectural Isolation:
- This module is strictly an offline experimentation pipeline.
- It is NOT connected to or referenced by Spring Boot backend, DocumentExtractionService,
  EligibilityEngine, or React frontend.
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
    print("Citizen Welfare Scheme Recommendation Model")
    print("==================================================")
    print(f"Loading model from: {model_path}")
    checkpoint = joblib.load(model_path)
    pipeline = checkpoint["pipeline"]
    trained_at = checkpoint.get("trained_at", "Unknown")
    classes = checkpoint.get("classes", [])
    print(f"Model trained at: {trained_at}")
    print(f"Pipeline steps: {[step[0] for step in pipeline.steps]}")
    print(f"Target scheme categories ({len(classes)} classes): {classes}")
    print("")

    df = pd.read_csv(data_path)
    target_col = "scheme_category"
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

    # Specific requested demonstration citizen profile
    demo_profile = pd.DataFrame([{
        "age": 22,
        "gender": "Female",
        "state": "Tamil Nadu",
        "income": 120000,
        "occupation": "Student",
        "category": "OBC",
        "rural": "Yes",
        "student": "Yes",
        "disability": "No",
    }])

    pred_category = pipeline.predict(demo_profile)[0]
    pred_probs = pipeline.predict_proba(demo_profile)[0]
    category_prob = pred_probs[list(pipeline.classes_).index(pred_category)] * 100.0

    print("==================================================")
    print("SAMPLE PREDICTION (STANDALONE RANDOM FOREST)")
    print("==================================================")
    print("Input Citizen Profile:")
    print("")
    print(f"Age: {demo_profile.iloc[0]['age']}")
    print(f"Gender: {demo_profile.iloc[0]['gender']}")
    print(f"State: {demo_profile.iloc[0]['state']}")
    print(f"Income: {demo_profile.iloc[0]['income']}")
    print(f"Occupation: {demo_profile.iloc[0]['occupation']}")
    print(f"Category: {demo_profile.iloc[0]['category']}")
    print(f"Rural: {demo_profile.iloc[0]['rural']}")
    print(f"Student: {demo_profile.iloc[0]['student']}")
    print(f"Disability: {demo_profile.iloc[0]['disability']}")
    print("")
    print("Predicted Scheme Category:")
    print(f"{pred_category}")
    print(f"(Confidence / Model Probability: {category_prob:.1f}%)")
    print("==================================================")
    print("")

    # Additional diverse sample citizen profiles for validation
    additional_profiles = pd.DataFrame([
        {
            "age": 45,
            "gender": "Male",
            "state": "Maharashtra",
            "income": 150000,
            "occupation": "Farmer",
            "category": "General",
            "rural": "Yes",
            "student": "No",
            "disability": "No",
        },
        {
            "age": 67,
            "gender": "Male",
            "state": "Uttar Pradesh",
            "income": 60000,
            "occupation": "Unemployed",
            "category": "SC",
            "rural": "Yes",
            "student": "No",
            "disability": "No",
        },
        {
            "age": 34,
            "gender": "Female",
            "state": "Karnataka",
            "income": 95000,
            "occupation": "Artisan",
            "category": "OBC",
            "rural": "No",
            "student": "No",
            "disability": "No",
        },
        {
            "age": 29,
            "gender": "Male",
            "state": "Kerala",
            "income": 80000,
            "occupation": "Unemployed",
            "category": "General",
            "rural": "No",
            "student": "No",
            "disability": "Yes",
        },
    ])

    extra_preds = pipeline.predict(additional_profiles)
    extra_probs = pipeline.predict_proba(additional_profiles)

    descriptions = [
        "Rural Farmer (Maharashtra, 45, Male, General, 1.5L Income)",
        "Elderly Citizen (Uttar Pradesh, 67, Male, SC, 60k Income)",
        "Urban Artisan (Karnataka, 34, Female, OBC, 95k Income)",
        "Person with Disability (Kerala, 29, Male, General, 80k Income)",
    ]

    print("Additional Demonstration Profiles:")
    for i, desc in enumerate(descriptions):
        cat = extra_preds[i]
        prob = extra_probs[i][list(pipeline.classes_).index(cat)] * 100.0
        print(f"Profile {i+1}: {desc}")
        print(f"  -> Predicted Scheme Category: {cat} ({prob:.1f}% confidence)")

    print("")
    print("==================================================")
    print("EVALUATION COMPLETE")
    print("==================================================")


if __name__ == "__main__":
    evaluate()
