from pymongo import MongoClient
import json

client = MongoClient("mongodb://schemebridge_user:31Rfp1OuAafIzBhm1Xu6G5p7@localhost:27017/schemebridge_scheme_db?authSource=admin")
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
    }
}

print(json.dumps(audit_result, indent=2))
