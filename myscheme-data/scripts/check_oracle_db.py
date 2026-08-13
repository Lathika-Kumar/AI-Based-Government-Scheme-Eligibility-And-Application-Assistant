import sys

try:
    import oracledb
    print("oracledb is installed")
    
    # Try connecting
    conn = oracledb.connect(user="system", password="system", dsn="localhost:1521/XEPDB1")
    print("Successfully connected to Oracle DB XEPDB1!")
    cursor = conn.cursor()
    
    cursor.execute("SELECT table_name FROM user_tables WHERE table_name LIKE 'SCHEME%'")
    tables = cursor.fetchall()
    print("Existing Scheme Tables:", [t[0] for t in tables])
    
    for table in ["SCHEMES", "SCHEME_CATEGORIES", "SCHEME_DEPARTMENTS", "SCHEME_BENEFITS", "SCHEME_ELIGIBILITY_RULES", "SCHEME_DOCUMENTS", "SCHEME_FAQS", "SCHEME_TAGS"]:
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            cnt = cursor.fetchone()[0]
            print(f"Table {table:25s} count: {cnt}")
        except Exception as e:
            print(f"Table {table:25s} error: {e}")

    # Inspect SCHEMES columns
    cursor.execute("SELECT column_name, data_type, data_length FROM user_tab_columns WHERE table_name = 'SCHEMES'")
    cols = cursor.fetchall()
    print("\nExisting SCHEMES Columns:")
    for col in cols:
        print(f"  - {col[0]:25s} {col[1]} ({col[2]})")

    cursor.close()
    conn.close()

except Exception as e:
    print(f"Oracle DB check exception: {e}")
