import os
import find_key

key = os.environ.get("MYSCHEME_API_KEY") or find_key.find_myscheme_key()
print("MYSCHEME_API_KEY:", key)
