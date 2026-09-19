#!/usr/bin/env python3
"""
SchemeBridge Visual OCR Bridge
Executes native Windows Media OCR on image files or piped image bytes.
Returns structured JSON with extracted lines, words, and bounding coordinates.
"""
import sys
import os
import io
import json

def run_ocr(image_input):
    try:
        from PIL import Image
        import winocr

        if isinstance(image_input, (bytes, bytearray)):
            img = Image.open(io.BytesIO(image_input))
        else:
            img = Image.open(image_input)

        res = winocr.recognize_pil_sync(img)

        lines = []
        for l in res.get("lines", []):
            line_text = l.get("text", "").strip()
            if not line_text:
                continue
            words = []
            min_x = 999999.0
            min_y = 999999.0
            max_x = 0.0
            max_y = 0.0
            for w in l.get("words", []):
                rect = w.get("bounding_rect", {})
                wx = float(rect.get("x", 0.0))
                wy = float(rect.get("y", 0.0))
                ww = float(rect.get("width", 0.0))
                wh = float(rect.get("height", 0.0))
                min_x = min(min_x, wx)
                min_y = min(min_y, wy)
                max_x = max(max_x, wx + ww)
                max_y = max(max_y, wy + wh)
                words.append({
                    "text": w.get("text", ""),
                    "x": wx,
                    "y": wy,
                    "width": ww,
                    "height": wh
                })
            lines.append({
                "text": line_text,
                "x": min_x if min_x < 999999.0 else 0.0,
                "y": min_y if min_y < 999999.0 else 0.0,
                "width": max(0.0, max_x - min_x) if min_x < 999999.0 else 0.0,
                "height": max(0.0, max_y - min_y) if min_y < 999999.0 else 0.0,
                "words": words
            })

        full_text = "\n".join(l["text"] for l in lines)
        return {
            "success": True,
            "text": full_text,
            "lines": lines
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "text": "",
            "lines": []
        }

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] != "--stdin":
        target = sys.argv[1]
        result = run_ocr(target)
    else:
        raw_bytes = sys.stdin.buffer.read()
        result = run_ocr(raw_bytes)

    print(json.dumps(result))
