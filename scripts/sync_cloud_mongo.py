#!/usr/bin/env python3
"""
SchemeBridge Cloud MongoDB Synchronization Utility
Transfers all local SchemeBridge database collections and GridFS documents to Cloud MongoDB (e.g. MongoDB Atlas / Render).
Ensures idempotent sync using upsert on _id.
"""

import os
import sys
import argparse
from pymongo import MongoClient, ReplaceOne
from pymongo.errors import PyMongoError

LOCAL_DEFAULT_URI = "mongodb://schemebridge_user:31Rfp1OuAafIzBhm1Xu6G5p7@localhost:27017/schemebridge_scheme_db?authSource=admin"

COLLECTIONS_TO_SYNC = [
    "schemes",
    "scheme_categories",
    "scheme_verified_data",
    "applications",
    "application_documents",
    "application_reviews",
    "application_events",
    "admin_audit_logs",
    "admin_settings",
    "citizen_profiles",
    "citizen_vault_documents",
    "document_verification_results",
    "grievances",
    "notifications",
    "portal_feedback",
    "recommendation_events",
    "database_sequences",
    "fs.files",
    "fs.chunks"
]

def sync_collections(source_uri, target_uri, target_db_name=None):
    print("=" * 60)
    print("SCHEMEBRIDGE MONGODB CLOUD SYNCHRONIZATION")
    print("=" * 60)

    try:
        source_client = MongoClient(source_uri)
        source_db = source_client.get_default_database()
        if source_db is None:
            source_db = source_client["schemebridge_scheme_db"]
    except Exception as e:
        print(f"[ERROR] Failed to connect to source local MongoDB: {e}")
        return False

    try:
        target_client = MongoClient(target_uri)
        if target_db_name:
            target_db = target_client[target_db_name]
        else:
            target_db = target_client.get_default_database()
            if target_db is None:
                target_db = target_client["schemebridge_scheme_db"]
        # Ping target
        target_client.admin.command('ping')
        print(f"[OK] Successfully connected to target Cloud MongoDB database: '{target_db.name}'")
    except Exception as e:
        print(f"[ERROR] Failed to connect to target Cloud MongoDB: {e}")
        return False

    print("\nStarting collection sync...\n")
    summary = []

    for col_name in COLLECTIONS_TO_SYNC:
        if col_name not in source_db.list_collection_names():
            continue

        src_col = source_db[col_name]
        tgt_col = target_db[col_name]

        src_count = src_col.count_documents({})
        if src_count == 0:
            summary.append((col_name, 0, tgt_col.count_documents({})))
            continue

        print(f"Syncing '{col_name}' ({src_count} documents)...", end="", flush=True)

        batch = []
        synced = 0
        batch_size = 500

        for doc in src_col.find():
            batch.append(ReplaceOne({"_id": doc["_id"]}, doc, upsert=True))
            if len(batch) >= batch_size:
                tgt_col.bulk_write(batch, ordered=False)
                synced += len(batch)
                batch = []

        if batch:
            tgt_col.bulk_write(batch, ordered=False)
            synced += len(batch)

        tgt_count = tgt_col.count_documents({})
        print(f" Done! [Target Total: {tgt_count}]")
        summary.append((col_name, src_count, tgt_count))

    print("\n" + "=" * 60)
    print(f"{'COLLECTION':<32} | {'LOCAL':<8} | {'CLOUD TOTAL':<12}")
    print("-" * 60)
    for col_name, src_c, tgt_c in summary:
        print(f"{col_name:<32} | {src_c:<8} | {tgt_c:<12}")
    print("=" * 60)
    print("\nSync completed successfully.")
    return True

def main():
    parser = argparse.ArgumentParser(description="Sync SchemeBridge MongoDB data to Cloud")
    parser.add_argument("--cloud-uri", dest="cloud_uri", default=os.environ.get("CLOUD_MONGODB_URI"),
                        help="Target Cloud MongoDB connection URI (or set CLOUD_MONGODB_URI env var)")
    parser.add_argument("--db-name", dest="db_name", default=os.environ.get("MONGODB_DATABASE", "schemebridge_scheme_db"),
                        help="Database name in target cloud MongoDB (default: schemebridge_scheme_db)")
    parser.add_argument("--source-uri", dest="source_uri", default=os.environ.get("LOCAL_MONGODB_URI", LOCAL_DEFAULT_URI),
                        help="Source local MongoDB connection URI")

    args = parser.parse_args()

    if not args.cloud_uri:
        print("[!] No Cloud MongoDB URI provided.")
        print("    Usage: python scripts/sync_cloud_mongo.py --cloud-uri \"<CLOUD_MONGODB_URI>\"")
        print("    Or export CLOUD_MONGODB_URI=\"<CLOUD_MONGODB_URI>\"")
        sys.exit(1)

    success = sync_collections(args.source_uri, args.cloud_uri, args.db_name)
    if not success:
        sys.exit(1)

if __name__ == "__main__":
    main()
