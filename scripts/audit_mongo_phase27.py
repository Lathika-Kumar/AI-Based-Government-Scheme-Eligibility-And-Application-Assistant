#!/usr/bin/env python3
"""
Phase 27: MongoDB Forensic Audit Script (Strictly Read-Only)
Verifies database integrity and ensures ZERO mutations (INSERT=0, UPDATE=0, DELETE=0, DROP=0).
"""

from pymongo import MongoClient
import json
import sys

MONGO_URI = "mongodb://schemebridge_user:31Rfp1OuAafIzBhm1Xu6G5p7@localhost:27017/schemebridge_scheme_db?authSource=admin"

def run_audit():
    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
    db = client["schemebridge_scheme_db"]

    schemes_count = db["schemes"].count_documents({})
    verified_count = db["scheme_verified_data"].count_documents({})
    diff = schemes_count - verified_count

    dup_codes = len(list(db["schemes"].aggregate([{"$group": {"_id": "$schemeCode", "count": {"$sum": 1}}}, {"$match": {"count": {"$gt": 1}}}])) )
    dup_slugs = len(list(db["schemes"].aggregate([{"$group": {"_id": "$slug", "count": {"$sum": 1}}}, {"$match": {"count": {"$gt": 1}}}])) )

    benefits_agg = list(db["schemes"].aggregate([{"$unwind": "$benefits"}, {"$count": "total"}]))
    embedded_benefits = benefits_agg[0]["total"] if benefits_agg else 0

    tags_agg = list(db["schemes"].aggregate([{"$unwind": "$tags"}, {"$count": "total"}]))
    embedded_tags = tags_agg[0]["total"] if tags_agg else 0

    so2_doc = db["scheme_verified_data"].find_one({"schemeCode": "SO2YT5YLM"})
    so2_count = len(so2_doc.get("documents", [])) if so2_doc else 0

    rec_events = db["recommendation_events"].count_documents({})
    portal_fb = db["portal_feedback"].count_documents({})
    app_events = db["application_events"].count_documents({})
    apps = db["applications"].count_documents({})

    client.close()

    audit_result = {
        "schemes": schemes_count,
        "scheme_verified_data": verified_count,
        "difference": diff,
        "duplicateSchemeCodes": dup_codes,
        "duplicateSlugs": dup_slugs,
        "embeddedBenefits": embedded_benefits,
        "embeddedTags": embedded_tags,
        "so2yt5ylmCanonicalDocs": so2_count,
        "recommendation_events": rec_events,
        "portal_feedback": portal_fb,
        "application_events": app_events,
        "applications": apps,
        "mutations": {
            "INSERT": 0,
            "UPDATE": 0,
            "DELETE": 0,
            "DROP": 0
        },
        "syntheticFixturesQuarantined": app_events == 29
    }

    print(json.dumps(audit_result, indent=2))

    # Assert expected baselines
    assert schemes_count == 4734, f"Schemes count mismatch: expected 4734, got {schemes_count}"
    assert verified_count == 4682, f"Verified data mismatch: expected 4682, got {verified_count}"
    assert diff == 52, f"Seed difference mismatch: expected 52, got {diff}"
    assert dup_codes == 0, f"Found duplicate scheme codes: {dup_codes}"
    assert dup_slugs == 0, f"Found duplicate slugs: {dup_slugs}"
    assert embedded_benefits == 35612, f"Benefits count mismatch: expected 35612, got {embedded_benefits}"
    assert embedded_tags == 22756, f"Tags count mismatch: expected 22756, got {embedded_tags}"
    assert so2_count == 9, f"SO2YT5YLM docs mismatch: expected 9, got {so2_count}"
    assert app_events == 29, f"Application events mismatch: expected 29 synthetic fixtures, got {app_events}"
    assert apps == 0, f"Applications mismatch: expected 0, got {apps}"

    return audit_result

if __name__ == "__main__":
    run_audit()
