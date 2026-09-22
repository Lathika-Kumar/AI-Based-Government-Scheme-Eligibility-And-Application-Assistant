#!/usr/bin/env python3
"""
SRS PDF Generator for SchemeBridge
Converts SRS_DOCUMENT.md into a high-quality IEEE Std 830-1998 PDF document
styled to match the Karpagam College of Engineering SRS Template.
"""

import os
import re
import subprocess
from markdown_it import MarkdownIt

WORKSPACE = "E:/SCHEMEBRIDGE"
MD_FILE = os.path.join(WORKSPACE, "SRS_DOCUMENT.md")
HTML_FILE = os.path.join(WORKSPACE, "SRS_DOCUMENT.html")
PDF_FILE = os.path.join(WORKSPACE, "SRS_DOCUMENT.pdf")
CHROME_PATH = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

def build_pdf():
    print(f"[*] Reading Markdown from {MD_FILE}...")
    with open(MD_FILE, "r", encoding="utf-8") as f:
        md_content = f.read()

    # Initialize markdown-it with table support
    md = MarkdownIt("commonmark").enable("table").enable("strikethrough")
    
    # Custom pre-processing for alerts / blockquotes
    body_html = md.render(md_content)

    # Post-process HTML for styling
    # 1. Style tables
    body_html = body_html.replace("<table>", '<div class="table-container"><table class="srs-table">')
    body_html = body_html.replace("</table>", "</table></div>")

    # 2. Convert horizontal rules to section dividers or page breaks where appropriate
    body_html = re.sub(r'<hr\s*/?>', '<div class="page-break"></div>', body_html)

    # 3. Add section badges or callout styles
    body_html = body_html.replace("<blockquote>", '<div class="callout-box">')
    body_html = body_html.replace("</blockquote>", '</div>')

    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>SchemeBridge - System Requirements Specification (SRS)</title>
    <style>
        @page {{
            size: A4 portrait;
            margin: 18mm 16mm 20mm 16mm;
            @top-center {{
                content: "SRS | SchemeBridge | Full Stack Java (AI-Integrated) Training Programme";
                font-family: 'Segoe UI', Arial, sans-serif;
                font-size: 8pt;
                color: #64748b;
                border-bottom: 1px solid #cbd5e1;
                padding-bottom: 4px;
                width: 100%;
            }}
            @bottom-left {{
                content: "Confidential — For Training & Evaluation Use Only | Karpagam College of Engineering";
                font-family: 'Segoe UI', Arial, sans-serif;
                font-size: 7.5pt;
                color: #94a3b8;
            }}
            @bottom-right {{
                content: "Page " counter(page);
                font-family: 'Segoe UI', Arial, sans-serif;
                font-size: 8pt;
                font-weight: 600;
                color: #475569;
            }}
        }}

        *, *:before, *:after {{
            box-sizing: border-box;
        }}

        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            font-size: 9.5pt;
            line-height: 1.5;
            color: #1e293b;
            background-color: #ffffff;
            margin: 0;
            padding: 0;
        }}

        /* Document Header / Cover Styling */
        .doc-header {{
            text-align: center;
            border-bottom: 3px solid #1e3a8a;
            padding-bottom: 12px;
            margin-bottom: 24px;
        }}

        .doc-header h1 {{
            font-size: 20pt;
            color: #1e3a8a;
            text-transform: uppercase;
            letter-spacing: 0.8px;
            margin: 0 0 6px 0;
            font-weight: 800;
        }}

        .doc-header h2 {{
            font-size: 13pt;
            color: #b45309;
            margin: 0 0 4px 0;
            font-weight: 600;
        }}

        .doc-header h3 {{
            font-size: 11pt;
            color: #475569;
            margin: 0;
            font-weight: 500;
        }}

        /* Section Headings */
        h1 {{
            font-size: 14pt;
            color: #0f172a;
            border-bottom: 2px solid #0284c7;
            padding-bottom: 4px;
            margin-top: 24px;
            margin-bottom: 12px;
            page-break-after: avoid;
        }}

        h2 {{
            font-size: 12pt;
            color: #1e3a8a;
            background-color: #f1f5f9;
            padding: 6px 10px;
            border-left: 4px solid #1e3a8a;
            margin-top: 20px;
            margin-bottom: 10px;
            page-break-after: avoid;
        }}

        h3 {{
            font-size: 10.5pt;
            color: #0369a1;
            margin-top: 14px;
            margin-bottom: 6px;
            page-break-after: avoid;
        }}

        h4 {{
            font-size: 9.8pt;
            color: #334155;
            margin-top: 10px;
            margin-bottom: 4px;
            page-break-after: avoid;
        }}

        p {{
            margin-top: 0;
            margin-bottom: 8px;
            text-align: justify;
        }}

        ul, ol {{
            margin-top: 0;
            margin-bottom: 8px;
            padding-left: 20px;
        }}

        li {{
            margin-bottom: 3px;
        }}

        /* Tables */
        .table-container {{
            width: 100%;
            margin-bottom: 14px;
            page-break-inside: auto;
        }}

        table.srs-table {{
            width: 100%;
            border-collapse: collapse;
            font-size: 8.5pt;
            page-break-inside: auto;
        }}

        table.srs-table tr {{
            page-break-inside: avoid;
            page-break-after: auto;
        }}

        table.srs-table th {{
            background: linear-gradient(135deg, #1e3a8a, #1e40af);
            color: #ffffff;
            font-weight: 600;
            text-align: left;
            padding: 6px 8px;
            border: 1px solid #cbd5e1;
            letter-spacing: 0.2px;
        }}

        table.srs-table td {{
            padding: 5px 8px;
            border: 1px solid #cbd5e1;
            vertical-align: top;
        }}

        table.srs-table tr:nth-child(even) td {{
            background-color: #f8fafc;
        }}

        table.srs-table tr:hover td {{
            background-color: #f1f5f9;
        }}

        /* Callout / Note Box */
        .callout-box {{
            background-color: #fef3c7;
            border-left: 4px solid #f59e0b;
            padding: 8px 12px;
            margin: 12px 0;
            border-radius: 0 4px 4px 0;
            font-size: 8.8pt;
            color: #78350f;
        }}

        .callout-box p {{
            margin: 0;
        }}

        img {{
            max-width: 100%;
            height: auto;
            display: block;
            margin: 14px auto;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
            page-break-inside: avoid;
        }}

        /* Code Blocks & Preformatted Text */
        pre {{
            background-color: #0f172a;
            color: #f8fafc;
            padding: 10px 12px;
            border-radius: 4px;
            font-family: "Consolas", "Courier New", monospace;
            font-size: 7.8pt;
            line-height: 1.35;
            overflow-x: auto;
            margin: 8px 0 12px 0;
            page-break-inside: avoid;
            border: 1px solid #334155;
        }}

        code {{
            font-family: "Consolas", "Courier New", monospace;
            font-size: 8pt;
            background-color: #f1f5f9;
            color: #0f172a;
            padding: 1px 4px;
            border-radius: 3px;
            border: 1px solid #e2e8f0;
        }}

        pre code {{
            background: none;
            color: inherit;
            padding: 0;
            border: none;
        }}

        /* Page Breaks */
        .page-break {{
            page-break-before: always;
            margin-top: 20px;
        }}

        /* Clean badges & indicators */
        strong {{
            color: #0f172a;
        }}

        .header-running {{
            font-size: 7.5pt;
            color: #64748b;
            display: flex;
            justify-content: space-between;
            border-bottom: 1px solid #e2e8f0;
            padding-bottom: 4px;
            margin-bottom: 12px;
        }}

        .footer-running {{
            font-size: 7.5pt;
            color: #94a3b8;
            display: flex;
            justify-content: space-between;
            border-top: 1px solid #e2e8f0;
            padding-top: 4px;
            margin-top: 16px;
        }}
    </style>
</head>
<body>
    <div class="header-running">
        <span>SRS Template | SchemeBridge</span>
        <span>Full Stack Java (AI-Integrated) Training Programme</span>
    </div>

    {body_html}

    <div class="footer-running">
        <span>Confidential — For Training Use Only | Karpagam College of Engineering</span>
        <span>SchemeBridge SRS v1.0</span>
    </div>
</body>
</html>
"""

    print(f"[*] Writing styled HTML to {HTML_FILE}...")
    with open(HTML_FILE, "w", encoding="utf-8") as f:
        f.write(full_html)

    print(f"[*] Compiling PDF using headless Chrome from {HTML_FILE}...")
    cmd = [
        CHROME_PATH,
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        f"--print-to-pdf={PDF_FILE}",
        HTML_FILE
    ]
    
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        print("[!] Error running Chrome headless:", proc.stderr)
        return False
    
    if os.path.exists(PDF_FILE):
        size_kb = os.path.getsize(PDF_FILE) / 1024.0
        print(f"[+] Successfully generated PDF: {PDF_FILE} ({size_kb:.2f} KB)")
        return True
    else:
        print("[!] PDF generation failed: target file not found.")
        return False

if __name__ == "__main__":
    success = build_pdf()
    if not success:
        exit(1)
