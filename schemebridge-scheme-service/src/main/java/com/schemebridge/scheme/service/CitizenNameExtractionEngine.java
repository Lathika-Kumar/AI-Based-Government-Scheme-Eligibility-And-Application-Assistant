package com.schemebridge.scheme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal Citizen / Document Holder Name Extraction Engine.
 *
 * Core Principles:
 * 1. Extraction, NOT Guessing: Determines the genuine document holder solely from
 *    document pixels, PDF text, OCR text, bounding boxes, and layout.
 * 2. Zero Profile Leakage: NEVER reads from or accepts CitizenProfile.
 * 3. Document-Type Independent: Universal across all document types (Aadhaar, Income,
 *    Caste, Community, Domicile, Birth, Education, Identity Proof, etc.).
 * 4. Multi-Signal Scoring: Combines semantic anchors, spatial/proximity layout,
 *    relationship context, repetition, OCR quality, and conservative artifact normalization.
 * 5. Strict False-Positive Protection: Rejects relatives (father, mother, spouse),
 *    authorities, locations, dates, numbers, and font corruptions.
 * 6. UNCERTAIN State: Returns null / UNCERTAIN when no candidate exceeds confidence threshold.
 */
@Service
@Slf4j
public class CitizenNameExtractionEngine {

    public record OcrWord(String text, double x, double y, double width, double height) {}

    public record OcrLine(String text, double x, double y, double width, double height, List<OcrWord> words) {}

    public record StructuredOcrData(String rawText, List<OcrLine> lines) {}

    public record NameCandidate(
            String rawCandidate,
            String normalizedName,
            String sourceAnchor,
            int semanticScore,
            int spatialScore,
            int documentStructureScore,
            int ocrQualityScore,
            int repetitionScore,
            int relationshipScore,
            int normalizationScore,
            int totalScore,
            boolean disqualified,
            String disqualificationReason
    ) {}

    public record ExtractionResult(
            String holderName,
            double confidence,
            String status, // "FOUND", "NOT_FOUND", "UNCERTAIN"
            List<NameCandidate> scoredCandidates,
            NameCandidate selectedCandidate
    ) {}

    // Minimum total score required to accept a candidate as the genuine document holder
    public static final int MIN_CONFIDENCE_THRESHOLD = 40;

    public static final String RELATIVE_KEYWORDS =
            "father(?:'?s)?(?:\\s+name)?|mother(?:'?s)?(?:\\s+name)?|husband(?:'?s)?(?:\\s+name)?|wife(?:'?s)?(?:\\s+name)?|spouse(?:'?s)?(?:\\s+name)?|guardian(?:'?s)?(?:\\s+name)?|parent(?:'?s)?(?:\\s+name)?|" +
            "s/o|d/o|w/o|c/o|son\\s+of|daughter\\s+of|wife\\s+of|husband\\s+of|child\\s+of|ward\\s+of|" +
            "पिता(?:\\s*(?:का|के))?(?:\\s*नाम)?|माता(?:\\s*(?:का|की))?(?:\\s*नाम)?|पति(?:\\s*का)?(?:\\s*नाम)?|पत्नी(?:\\s*का)?(?:\\s*नाम)?|पुत्र|पुत्री|" +
            "தந்தை(?:யின்)?(?:\\s*பெயர்)?|தகப்பனார்(?:\\s*பெயர்)?|தாய்(?:யின்)?(?:\\s*பெயர்)?|கணவர்(?:\\s*பெயர்)?|மனைவி(?:\\s*பெயர்)?|மகள்|மகன்|" +
            "తండ్రి(?:\\s*పేరు)?|తల్లి(?:\\s*పేరు)?|భర్త(?:\\s*పేరు)?|భార్య(?:\\s*పేరు)?|కుమారుడు|కుమార్తె|" +
            "ತಂದೆ(?:ಯ)?(?:\\s*ಹೆಸರು)?|ತಾಯಿ(?:ಯ)?(?:\\s*ಹೆಸರು)?|ಪತಿ(?:ಯ)?(?:\\s*ಹೆಸರು)?|ಪತ್ನಿ(?:ಯ)?(?:\\s*ಹೆಸರು)?|ಮಗ|ಮಗಳು";

    // ── Semantic Anchor Patterns ──────────────────────────────────────────────
    private static final Pattern PATTERN_EXPLICIT_NAME_LABEL = Pattern.compile(
            "(?i)(?:Name of (?:the )?(?:Applicant|Certificate Holder|Person|Citizen|Beneficiary|Student|Candidate|Holder|Child|Ward)|" +
            "Applicant Name|Beneficiary Name|Citizen Name|Holder Name|Card Holder|Certificate Holder|" +
            "Person Name|Student Name|Candidate Name|Member Name|Individual Name|Child Name|Ward Name|" +
            "Holder's Name|Applicant's Name|Name of Holder|Name of the Holder|" +
            "விண்ணப்பதாரர் பெயர்|பயனாளி பெயர்|சான்றிதழ் வைத்திருப்பவர்|மாணவர் பெயர்|குழந்தை பெயர்|" +
            "आवेदक का नाम|लाभार्थी का नाम|प्रमाणपत्र धारक|छात्र का नाम|नागरिक का नाम|" +
            "దరఖాస్తుదారు పేరు|లబ్దిదారుని పేరు|విద్యార్థి పేరు|పౌరుని పేరు|" +
            "ಅರ್ಜಿದಾರರ ಹೆಸರು|ಫಲಾನುಭವಿಯ ಹೆಸರು|ವಿದ್ಯಾರ್ಥಿಯ ಹೆಸರು|ನಾಗರಿಕರ ಹೆಸರು|" +
            "(?:^|[\r\n])\\s*(?:பெயர்|नाम|పేరు|ಹೆಸರು))[ \\t:\\-]*([A-Za-z \\t.]{2,40})(?:\r?\n|$|,)"
    );

    private static final Pattern PATTERN_BASIC_NAME_LABEL = Pattern.compile(
            "(?i)(?:^|[\r\n])\\s*(?:Name|नाम|பெயர்|పేరు|ಹೆಸರು)\\s*[:\\-]+\\s*([A-Za-z \\t.]{3,40})(?:\r?\n|$)"
    );

    private static final Pattern PATTERN_CERTIFY_STATEMENT = Pattern.compile(
            "(?i)(?:This is to certify[.\\s]*(?:that)?|சான்றிதழ்[\\s:]*|சான்றிதழ் அளிக்கப்படுகிறது[\\s:]*|சான்றளிக்கப்படுகிறது[\\s:]*|வழங்கப்படுகிறது[\\s:]*|" +
            "यह प्रमाणित किया जाता है कि[\\s:]*|प्रमाणित किया जाता है ਕਿ?[\\s:]*|ధృవీకరించడమైనది[\\s:]*|ದೃಢೀಕರಿಸಲಾಗಿದೆ[\\s:]*|" +
            "Certified that|This is certified that|It is certified that|hereby certify that|issued in favou?r of|This certificate is issued to|awarded to)[\\s:]*" +
            "(?:(?:Selvi|Thiru|Tmt|Shri|Sri|Smt|Mr|Mrs|Ms|Miss|Dr|Kum|Kumari|Master|செல்வி|திரு|திருமதி|श्री|श्रीमती|सुश्री|कुमारी)[.\\s]+)?" +
            "([A-Za-z\\s.]{2,35}?)" +
            "(?=\\s+(?:daughter\\s+of|son\\s+of|wife\\s+of|husband\\s+of|child\\s+of|ward\\s+of|D/O|S/O|W/O|C/O|" +
            "தந்தை|தாய்|மகள்|மகன்|மனைவி|கணவர்|வசிக்கும்|இருப்பிடம்|" +
            "पिता|माता|पुत्र|पुत्री|पति|पत्नी|निवासी|" +
            "తండ్రి|తల్లి|కుమారుడు|కుమార్తె|భార్య|భర్త|" +
            "ತಂದೆ|ತಾಯಿ|ಮಗ|ಮಗಳು|ಪತ್ನಿ|ಪತಿ|" +
            "residing|residence|is\\s+a\\s+resident|belongs\\s+to|holding|whose|resides|Door\\s+No|aged|inhabitant|an\\s+inhabitant|studying|having|has\\s+passed|completed|bearing)\\b)"
    );

    private static final Pattern PATTERN_CERTIFY_FALLBACK = Pattern.compile(
            "(?i)(?:This is to certify[.\\s]*(?:that)?|சான்றிதழ்[\\s:]*|சான்றிதழ் அளிக்கப்படுகிறது[\\s:]*|சான்றளிக்கப்படுகிறது[\\s:]*|வழங்கப்படுகிறது[\\s:]*|" +
            "यह प्रमाणित किया जाता है कि[\\s:]*|प्रमाणित किया जाता है ਕਿ?[\\s:]*|ధృవీకరించడమైనది[\\s:]*|ದೃಢೀಕರಿಸಲಾಗಿದೆ[\\s:]*|" +
            "Certified that|This is certified that|It is certified that|hereby certify that|issued in favou?r of|This certificate is issued to|awarded to)[\\s:]*" +
            "(?:(?:Selvi|Thiru|Tmt|Shri|Sri|Smt|Mr|Mrs|Ms|Miss|Dr|Kum|Kumari|Master|செல்வி|திரு|திருமதி|श्री|श्रीमती|सुश्री|कुमारी)[.\\s]+)?" +
            "([A-Za-z\\s.]{2,35}?)(?=\\s*[^A-Za-z\\s.]|\r?\n|$|,|\\.)"
    );

    private static final Pattern PATTERN_HONORIFIC_RELATION = Pattern.compile(
            "(?i)\\b(Selvi|Thiru|Tmt|Shri|Sri|Smt|Mr|Mrs|Ms|Miss|Dr|Kum|Kumari|Master)[.\\s]+" +
            "([A-Za-z\\s.]{2,35}?)" +
            "(?=\\s+(?:daughter\\s+of|son\\s+of|wife\\s+of|husband\\s+of|child\\s+of|ward\\s+of|D/O|S/O|W/O|C/O)\\b)"
    );

    private static final Pattern PATTERN_BENEFICIARY_APPLICANT = Pattern.compile(
            "(?i)(?:^|[\r\n])\\s*(?:Applicant|Beneficiary|Candidate|Citizen|Student)[ \\t:\\-]*([A-Za-z \\t.]{2,35})(?:\r?\n|$)"
    );

    private static final Pattern PATTERN_TABLE_HEADER = Pattern.compile(
            "(?i)(?:Name of (?:the )?(?:family )?Member|Family Members|Family Details|குடும்ப உறுப்பினர்|परिवार के सदस्य)"
    );

    private static final Pattern PATTERN_DOB_LABEL = Pattern.compile(
            "(?i)(?:DOB|Date of Birth|Birth Date|Year of Birth|பிறந்த\\s*தேதி|பிறந்த\\s*நாள்|जन्म\\s*तारीख|जन्म\\s*वर्ष|जन्म\\s*तिथि|పుట్టిన\\s*తేదీ|ಹುಟ್ಟಿದ\\s*ದಿನಾಂಕ)"
    );

    private static final Pattern PATTERN_GENDER_LABEL = Pattern.compile(
            "(?i)\\b(MALE|FEMALE|FERNALE|TRANSGENDER|ஆண்|பெண்|पुरुष|महिला|पुरुषుడు|స్త్రీ|ಪುರುಷ|ಮಹಿಳೆ)\\b"
    );

    private static final Pattern PATTERN_STANDALONE_DATE = Pattern.compile(
            "\\b([0-9]{2}[/\\-.][0-9]{2}[/\\-.][0-9]{4}|[0-9]{4}[/\\-.][0-9]{2}[/\\-.][0-9]{2})\\b"
    );

    // Font CMap corruption detection: intra-word lowercase followed by uppercase
    private static final Pattern PATTERN_INTRAWORD_MIXED_CASE = Pattern.compile("[a-z][A-Z]");

    // ── Negative Word Repositories ───────────────────────────────────────────
    private static final Set<String> GOVERNMENT_AUTHORITY_WORDS = Set.of(
            "government", "govt", "india", "state", "administration", "department",
            "revenue", "authority", "tahsildar", "collector", "officer", "designation",
            "office", "ministry", "competent", "uidai", "commissioner", "deputy",
            "taluk", "district", "tehsil", "mandal", "corporation", "panchayat", "municipality",
            "thority", "ority", "authorit", "iogfn", "odviae", "ramen", "ramenatu",
            "quarl", "divisional", "magistrate", "secretariat"
    );

    private static final Set<String> LOCATION_WORDS = Set.of(
            "street", "road", "door", "district", "taluk", "tehsil", "mandal",
            "village", "town", "nagar", "colony", "sector", "block", "ward",
            "post", "pin", "area", "vtc", "po", "cuddalore", "chennai", "delhi",
            "mumbai", "kolkata", "bangalore", "hyderabad", "tamil", "nadu",
            "karnataka", "kerala", "andhra", "pradesh", "telangana", "srimushnam", "kozhai"
    );

    private static final Set<String> DOCUMENT_METADATA_WORDS = Set.of(
            "certificate", "document", "application", "registration", "reference",
            "number", "aadhaar", "aadhar", "pan", "income", "caste", "community",
            "domicile", "residence", "birth", "death", "ration", "passbook", "signature",
            "signed", "digitally", "barcode", "qr", "seal", "reading", "portal",
            "online", "genuineness", "validity", "total", "remarks", "details",
            "serial", "annual", "rupees", "wages", "salary", "source", "date", "valid",
            "powered", "digilocker", "railways", "airports", "zoom", "proof",
            "member", "members", "family", "ofthefamily", "be on", "be", "on"
    );

    private static final Set<String> COMMON_ENGLISH_WORDS = Set.of(
            "this", "that", "these", "those", "have", "been", "hereby", "certified",
            "certify", "stating", "based", "furnished", "below", "following", "between",
            "under", "above", "with", "from", "into", "onto", "upon", "about", "after",
            "before", "during", "while", "where", "which", "whose", "whom", "what",
            "there", "their", "they", "them", "then", "than", "also", "only", "such",
            "some", "many", "more", "most", "other", "every", "each", "both", "either",
            "neither", "male", "female", "gender", "transgender", "year", "month", "day"
    );

    /**
     * Primary entry point for extracting the genuine citizen/holder name.
     */
    public ExtractionResult extractCitizenHolderName(
            String rawText,
            StructuredOcrData ocrData,
            String docTypeHint
    ) {
        return extractCitizenHolderName(rawText, null, ocrData, docTypeHint);
    }

    /**
     * Universal extraction entry point with dual-source reconciliation (PDF_TEXT + OCR_VISUAL).
     */
    public ExtractionResult extractCitizenHolderName(
            String pdfText,
            String visualOcrText,
            StructuredOcrData ocrData,
            String docTypeHint
    ) {
        String combinedText = buildCombinedText(pdfText, visualOcrText, ocrData);
        if (combinedText == null || combinedText.isBlank()) {
            return new ExtractionResult(null, 0.0, "NOT_FOUND", Collections.emptyList(), null);
        }

        // 1. Generate Raw Candidates from All Available Signals
        List<RawCandidate> rawCandidates = generateCandidates(pdfText, visualOcrText, ocrData, combinedText);

        if (rawCandidates.isEmpty()) {
            return new ExtractionResult(null, 0.0, "UNCERTAIN", Collections.emptyList(), null);
        }

        // 2. Score Candidates using Multi-Signal Analysis
        List<NameCandidate> scoredCandidates = new ArrayList<>();
        for (RawCandidate rc : rawCandidates) {
            NameCandidate scored = scoreCandidate(rc, combinedText, pdfText, visualOcrText, rawCandidates);
            scoredCandidates.add(scored);
        }

        // 3. Deduplicate and select highest-scoring qualified candidate
        Map<String, NameCandidate> bestByNormalized = new LinkedHashMap<>();
        for (NameCandidate c : scoredCandidates) {
            String norm = c.normalizedName();
            if (norm == null || norm.isBlank()) continue;
            NameCandidate existing = bestByNormalized.get(norm);
            if (existing == null || c.totalScore() > existing.totalScore()) {
                bestByNormalized.put(norm, c);
            }
        }

        List<NameCandidate> sorted = new ArrayList<>(bestByNormalized.values());
        sorted.sort((a, b) -> Integer.compare(b.totalScore(), a.totalScore()));

        // Filter qualified candidates: must not be disqualified and score >= MIN_CONFIDENCE_THRESHOLD
        NameCandidate best = null;
        for (NameCandidate c : sorted) {
            if (!c.disqualified() && c.totalScore() >= MIN_CONFIDENCE_THRESHOLD) {
                best = c;
                break;
            }
        }

        if (best == null) {
            log.info("No candidate met minimum confidence threshold ({}). Top candidate: {}",
                    MIN_CONFIDENCE_THRESHOLD, sorted.isEmpty() ? "none" : sorted.get(0).normalizedName() + " (" + sorted.get(0).totalScore() + ")");
            return new ExtractionResult(null, 0.0, "UNCERTAIN", sorted, null);
        }

        double confidence = calculateConfidence(best.totalScore());
        log.info("Selected document holder: '{}' (Score: {}, Anchor: {}, Confidence: {})",
                best.normalizedName(), best.totalScore(), best.sourceAnchor(), confidence);

        return new ExtractionResult(best.normalizedName(), confidence, "FOUND", sorted, best);
    }

    private double calculateConfidence(int score) {
        if (score >= 90) return 0.98;
        if (score >= 70) return 0.95;
        if (score >= 50) return 0.88;
        if (score >= MIN_CONFIDENCE_THRESHOLD) return 0.75;
        return 0.40;
    }

    private String buildCombinedText(String pdfText, String visualOcrText, StructuredOcrData ocrData) {
        StringBuilder sb = new StringBuilder();
        if (pdfText != null && !pdfText.isBlank()) {
            sb.append(pdfText.trim());
        }
        if (visualOcrText != null && !visualOcrText.isBlank()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(visualOcrText.trim());
        }
        if (ocrData != null && ocrData.rawText() != null && !ocrData.rawText().isBlank()) {
            String ocrTxt = ocrData.rawText().trim();
            if (!sb.toString().contains(ocrTxt)) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(ocrTxt);
            }
        }
        return sb.toString();
    }

    private record RawCandidate(
            String text,
            String sourceAnchor,
            int baseSemanticScore,
            int lineIndex,
            boolean isSelfRelation,
            boolean isParentRelation
    ) {}

    /**
     * Harvests potential person-name candidates across all document layouts.
     */
    private List<RawCandidate> generateCandidates(
            String pdfText,
            String visualOcrText,
            StructuredOcrData ocrData,
            String fullText
    ) {
        List<RawCandidate> list = new ArrayList<>();
        String clean = fullText.replace('\u00A0', ' ');

        // 1. Explicit Labels: "Applicant Name: Lathika", "Holder Name: ...", "பெயர்: ..."
        Matcher mExp = PATTERN_EXPLICIT_NAME_LABEL.matcher(clean);
        while (mExp.find()) {
            String raw = mExp.group(1);
            list.add(new RawCandidate(raw, "EXPLICIT_NAME_LABEL", 100, -1, false, false));
        }

        // Basic Name Label: "Name: Lathika"
        Matcher mBasic = PATTERN_BASIC_NAME_LABEL.matcher(clean);
        while (mBasic.find()) {
            String raw = mBasic.group(1);
            list.add(new RawCandidate(raw, "BASIC_NAME_LABEL", 95, -1, false, false));
        }

        // 2. Certification Statements: "This is to certify that Selvi Lathika daughter of..."
        Matcher mCert = PATTERN_CERTIFY_STATEMENT.matcher(clean);
        while (mCert.find()) {
            String raw = mCert.group(1);
            list.add(new RawCandidate(raw, "CERTIFY_STATEMENT", 95, -1, false, false));
        }

        // Certification Fallback (without relationship lookahead)
        Matcher mCertFb = PATTERN_CERTIFY_FALLBACK.matcher(clean);
        while (mCertFb.find()) {
            String raw = mCertFb.group(1);
            list.add(new RawCandidate(raw, "CERTIFY_FALLBACK", 85, -1, false, false));
        }

        // 3. Standalone Honorific + Relation: "Selvi Lathika daughter of Thiru Kumar"
        Matcher mHon = PATTERN_HONORIFIC_RELATION.matcher(clean);
        while (mHon.find()) {
            String raw = mHon.group(2);
            list.add(new RawCandidate(raw, "HONORIFIC_RELATION", 90, -1, false, false));
        }

        // 4. Beneficiary / Applicant label: "Applicant: Lathika"
        Matcher mBen = PATTERN_BENEFICIARY_APPLICANT.matcher(clean);
        while (mBen.find()) {
            String raw = mBen.group(1);
            list.add(new RawCandidate(raw, "BENEFICIARY_APPLICANT", 95, -1, false, false));
        }

        // 5. Family Table Rows: "Lathika - Self", "Kumar - Father"
        List<String[]> tableRows = extractFamilyTableRows(clean);
        for (String[] row : tableRows) {
            String name = row[0];
            String rel = row[1];
            boolean isSelf = rel.matches("(?i).*(?:self|selt|applicant|holder|சுய|स्वयं).*");
            boolean isParent = rel.matches("(?i).*(?:father|mother|son|daughter|wife|husband|தந்தை|தாய்|पिता|माता).*");
            list.add(new RawCandidate(name, isSelf ? "TABLE_SELF" : "TABLE_MEMBER", isSelf ? 80 : 5, -1, isSelf, isParent));
        }

        // 6. Aadhaar "To" line anchor: "To\nLathika"
        String[] lines = clean.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.matches("(?i)^To\\s*$") && i + 1 < lines.length) {
                list.add(new RawCandidate(lines[i + 1].trim(), "AADHAAR_TO_HEADER", 75, i + 1, false, false));
            } else if (l.matches("(?i)^To\\s*[:\\-]\\s*([A-Za-z\\s.]{3,40})$") || l.matches("(?i)^To\\s+([A-Za-z\\s.]{3,40})$")) {
                String c = l.replaceFirst("(?i)^To\\s*[:\\-]?\\s*", "").trim();
                list.add(new RawCandidate(c, "AADHAAR_TO_HEADER", 75, i, false, false));
            }
        }

        // 7. Spatial / Proximity Candidates near DOB and Gender
        extractProximityCandidates(lines, list);

        // 8. Visual OCR line inspection with bounding coordinates
        if (ocrData != null && ocrData.lines() != null) {
            extractSpatialOcrCandidates(ocrData.lines(), list);
        }

        return list;
    }

    private void extractProximityCandidates(String[] lines, List<RawCandidate> list) {
        Pattern relPattern = Pattern.compile("(?i).*\\b(?:" + RELATIVE_KEYWORDS + ")\\b.*");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            boolean isDob = PATTERN_DOB_LABEL.matcher(line).find() || PATTERN_STANDALONE_DATE.matcher(line).find();
            boolean isCertDate = line.matches("(?i).*(?:validity|period|issued|expiry|certificate|tn-|up/|mh/|barcode|address).*");

            if (isDob && !isCertDate) {
                // Line directly before DOB (typical in DigiLocker & Aadhaar cards: e.g. "Lathika K\n2007-03-22\nFemale")
                if (i > 0) {
                    String prevLine = lines[i - 1].trim();
                    boolean isRelative = relPattern.matcher(prevLine).find();
                    if (!isRelative) {
                        list.add(new RawCandidate(prevLine, "DIRECTLY_BEFORE_DOB", 90, i - 1, false, false));
                    } else if (i > 1) {
                        // The line directly before DOB is father/mother/spouse. The applicant is typically the line above that!
                        String lineAboveParent = lines[i - 2].trim();
                        if (!relPattern.matcher(lineAboveParent).find() && !lineAboveParent.matches("(?i).*(?:government|india|state|authority).*")) {
                            list.add(new RawCandidate(lineAboveParent, "APPLICANT_ABOVE_PARENT", 85, i - 2, false, false));
                        }
                    }
                }
                // Line directly after DOB
                if (i + 1 < lines.length && !PATTERN_GENDER_LABEL.matcher(lines[i + 1]).find()) {
                    String nextLine = lines[i + 1].trim();
                    if (!relPattern.matcher(nextLine).find()) {
                        list.add(new RawCandidate(nextLine, "DIRECTLY_AFTER_DOB", 60, i + 1, false, false));
                    }
                }
                // 2 lines before DOB (i - 2)
                if (i > 1) {
                    String lineMinus2 = lines[i - 2].trim();
                    if (!relPattern.matcher(lineMinus2).find() && !lineMinus2.matches("(?i).*(?:government|india|state|authority).*")) {
                        list.add(new RawCandidate(lineMinus2, "NEAR_DOB_MINUS_2", 85, i - 2, false, false));
                    }
                }
                // 3 lines before DOB (i - 3)
                if (i > 2) {
                    String lineMinus3 = lines[i - 3].trim();
                    if (!relPattern.matcher(lineMinus3).find() && !lineMinus3.matches("(?i).*(?:government|india|state|authority).*")) {
                        list.add(new RawCandidate(lineMinus3, "NEAR_DOB_MINUS_3", 80, i - 3, false, false));
                    }
                }
            }
        }
    }

    private void extractSpatialOcrCandidates(List<OcrLine> ocrLines, List<RawCandidate> list) {
        // Find DOB line bounding box
        OcrLine dobLine = null;
        for (OcrLine ol : ocrLines) {
            if (PATTERN_DOB_LABEL.matcher(ol.text()).find() || PATTERN_STANDALONE_DATE.matcher(ol.text()).find()) {
                if (!ol.text().matches("(?i).*(?:validity|issued|expiry|barcode).*")) {
                    dobLine = ol;
                    break;
                }
            }
        }

        for (int i = 0; i < ocrLines.size(); i++) {
            OcrLine ol = ocrLines.get(i);
            String text = ol.text().trim();
            if (text.length() >= 3 && text.matches("^[A-Za-z\\s.\\-']+$")) {
                // Check vertical spatial proximity to DOB
                if (dobLine != null) {
                    double dy = dobLine.y() - ol.y();
                    double dx = Math.abs(dobLine.x() - ol.x());
                    // Directly above DOB within 150px and horizontally aligned within 100px
                    if (dy > 0 && dy < 150 && dx < 100) {
                        list.add(new RawCandidate(text, "SPATIAL_ABOVE_DOB", 90, i, false, false));
                    }
                }
            }
        }
    }

    private List<String[]> extractFamilyTableRows(String cleanText) {
        List<String[]> rows = new ArrayList<>();
        String[] lines = cleanText.split("\\r?\\n");
        boolean inTable = false;
        int linesAfterHeader = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (PATTERN_TABLE_HEADER.matcher(line).find()) {
                inTable = true;
                linesAfterHeader = 0;
                continue;
            }
            if (inTable) {
                linesAfterHeader++;
                if (linesAfterHeader > 20 || line.matches("(?i).*(?:Total|Source|RS\\.|validity|This is to certify).*")) {
                    inTable = false;
                    continue;
                }
                Matcher mRow = Pattern.compile("(?i)^([A-Za-z\\s.]{3,30})\\s+(Self|Selt|Mother|Father|Son|Daughter|Wife|Husband|சுய|தந்தை|தாய்)\\b").matcher(line);
                if (mRow.find()) {
                    rows.add(new String[]{mRow.group(1).trim(), mRow.group(2).trim()});
                } else if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    if (nextLine.matches("(?i)^(?:Self|Selt|Mother|Father|Son|Daughter|Wife|Husband|சுய|தந்தை|தாய்)\\b.*")) {
                        rows.add(new String[]{line, nextLine});
                    }
                }
            }
        }
        return rows;
    }

    /**
     * Multi-Signal Candidate Scoring Engine.
     */
    private NameCandidate scoreCandidate(
            RawCandidate rc,
            String fullText,
            String pdfText,
            String visualOcrText,
            List<RawCandidate> allRaw
    ) {
        String raw = rc.text();
        String cleaned = cleanRawCandidate(raw);
        String normalized = normalizePersonNameCandidate(cleaned);

        int semanticScore = rc.baseSemanticScore();
        int spatialScore = 0;
        int documentStructureScore = 0;
        int ocrQualityScore = 0;
        int repetitionScore = 0;
        int relationshipScore = 0;
        int normalizationScore = 0;

        boolean disqualified = false;
        String disqualificationReason = null;

        // 1. Validate cleaned/normalized string
        if (normalized == null || normalized.isBlank() || normalized.length() < 2) {
            return new NameCandidate(raw, "", rc.sourceAnchor(), 0, 0, 0, 0, 0, 0, 0, -100, true, "Empty candidate");
        }

        String lowerNorm = normalized.toLowerCase();

        // 2. Disqualification: Font CMap corruption (intra-word mixed case, 3+ consecutive letters)
        if (PATTERN_INTRAWORD_MIXED_CASE.matcher(raw).find() || PATTERN_INTRAWORD_MIXED_CASE.matcher(normalized).find()) {
            return new NameCandidate(raw, normalized, rc.sourceAnchor(), 0, 0, 0, -100, 0, 0, 0, -100, true, "Font CMap corruption (mixed case)");
        }
        if (raw.matches(".*[a-z][A-Z]{2,}.*") || normalized.matches(".*[a-z][A-Z]{2,}.*")) {
            return new NameCandidate(raw, normalized, rc.sourceAnchor(), 0, 0, 0, -100, 0, 0, 0, -100, true, "Font glyph corruption (e.g. bTTT)");
        }
        if (lowerNorm.matches(".*([a-z])\\1{2,}.*")) {
            return new NameCandidate(raw, normalized, rc.sourceAnchor(), 0, 0, 0, -100, 0, 0, 0, -100, true, "Repeated character artifact");
        }

        // Signature / Digital seal metadata rejection
        if (lowerNorm.contains("digitally") || lowerNorm.contains("signed") || lowerNorm.contains("signature")) {
            return new NameCandidate(raw, normalized, rc.sourceAnchor(), 0, 0, 0, -100, 0, 0, 0, -100, true, "Signature / Digitally signed metadata");
        }

        // 3. Disqualification: Vowel check (every word of 2+ chars must have at least one vowel)
        String[] tokens = normalized.split("\\s+");
        for (String t : tokens) {
            String alphaOnly = t.replaceAll("[^A-Za-z]", "").toLowerCase();
            if (alphaOnly.length() >= 2 && !alphaOnly.matches(".*[aeiouy].*")) {
                return new NameCandidate(raw, normalized, rc.sourceAnchor(), 0, 0, 0, -100, 0, 0, 0, -100, true, "Vowelless token: " + t);
            }
        }

        // Single word of 4+ chars with < 15% vowels (consonant cluster e.g. dfgh)
        if (tokens.length == 1 && normalized.length() >= 4) {
            long vowels = lowerNorm.chars().filter(c -> "aeiouy".indexOf(c) >= 0).count();
            if ((double) vowels / normalized.length() < 0.15) {
                return new NameCandidate(raw, normalized, rc.sourceAnchor(), 0, 0, 0, -100, 0, 0, 0, -100, true, "Consonant cluster / low vowel ratio");
            }
        }

        // 4. Disqualification / Heavy Penalty: Government & Authority Words
        for (String w : GOVERNMENT_AUTHORITY_WORDS) {
            if (lowerNorm.equals(w) || lowerNorm.matches(".*\\b" + Pattern.quote(w) + "\\b.*")) {
                disqualified = true;
                disqualificationReason = "Authority / Government word: " + w;
                semanticScore -= 100;
                break;
            }
        }

        // 5. Disqualification / Heavy Penalty: Location & Address Words
        for (String loc : LOCATION_WORDS) {
            if (lowerNorm.equals(loc) || lowerNorm.matches(".*\\b" + Pattern.quote(loc) + "\\b.*")) {
                disqualified = true;
                disqualificationReason = "Location / Address word: " + loc;
                documentStructureScore -= 100;
                break;
            }
        }

        // Address line context: if raw candidate was inside an address block
        if (fullText.matches("(?i).*(?:Address|residing\\s+at)[^\\n]*" + Pattern.quote(normalized) + ".*")) {
            documentStructureScore -= 80;
        }

        // 6. Disqualification: Document Metadata & Common English Stopwords
        for (String meta : DOCUMENT_METADATA_WORDS) {
            if (lowerNorm.equals(meta) || lowerNorm.matches(".*\\b" + Pattern.quote(meta) + "\\b.*")) {
                disqualified = true;
                disqualificationReason = "Document metadata: " + meta;
                semanticScore -= 100;
                break;
            }
        }
        for (String ce : COMMON_ENGLISH_WORDS) {
            if (lowerNorm.equals(ce) || lowerNorm.matches("^" + Pattern.quote(ce) + "$")) {
                disqualified = true;
                disqualificationReason = "Common English stopword: " + ce;
                semanticScore -= 100;
                break;
            }
        }

        // 7. Numbers and Dates check
        if (raw.matches(".*\\d.*")) {
            disqualified = true;
            disqualificationReason = "Contains digits / date";
            ocrQualityScore -= 100;
        }

        // 8. Sentence / Excessive length check (names are 1 to 4 words, <= 35 chars)
        if (tokens.length > 4 || normalized.length() > 35) {
            disqualified = true;
            disqualificationReason = "Excessive length or sentence";
            ocrQualityScore -= 50;
        }

        // 9. Relationship Context Scoring (CRUCIAL):
        // Is this candidate someone else's relative (father, mother, husband, wife)?
        if (rc.isParentRelation()) {
            relationshipScore -= 100;
            disqualified = true;
            disqualificationReason = "Relative relation (father/mother/spouse)";
        } else if (rc.isSelfRelation()) {
            relationshipScore += 30;
        } else {
            // Check if fullText associates this candidate as a relative on the same line or context:
            Pattern parentPattern = Pattern.compile(
                    "(?i)(?:" + RELATIVE_KEYWORDS + ")[ \\t]*[:\\-]?[ \\t]*" +
                    "(?:of[ \\t]+)?(?:thiru|tmt|smt|mr|mrs|ms|dr|late[ \\t]+)?[ \\t]*\\b" + Pattern.quote(normalized) + "\\b"
            );
            Pattern parentLinePattern = Pattern.compile(
                    "(?i)(?:" + RELATIVE_KEYWORDS + ")[ \\t]*[:\\-].*\\b" + Pattern.quote(normalized) + "\\b"
            );
            if (parentPattern.matcher(fullText).find() || parentLinePattern.matcher(fullText).find()) {
                relationshipScore -= 100;
                disqualified = true;
                disqualificationReason = "Relative name in document context";
            }
        }

        // 10. Spatial & Proximity Signals
        if ("DIRECTLY_BEFORE_DOB".equals(rc.sourceAnchor()) || "SPATIAL_ABOVE_DOB".equals(rc.sourceAnchor())) {
            spatialScore += 45;
            // Check if Gender is also near (within next 2 lines)
            if (fullText.matches("(?i).*" + Pattern.quote(raw) + ".*(?:DOB|Birth|[0-9]{4}).*(?:Female|Male|பெண்|ஆண்|महिला|पुरुष).*")) {
                spatialScore += 45;
            }
        } else if ("AADHAAR_TO_HEADER".equals(rc.sourceAnchor())) {
            spatialScore += 30;
        }

        // Standalone line bonus: line contains just this name without other clutter
        if (raw.matches("^[A-Za-z\\s.\\-']+$") && tokens.length <= 3) {
            documentStructureScore += 20;
        }

        // 11. Repetition Score: candidate appears across multiple anchors or in both PDF & Visual OCR
        int occurrences = 0;
        for (RawCandidate other : allRaw) {
            String oClean = cleanRawCandidate(other.text());
            String oNorm = normalizePersonNameCandidate(oClean);
            if (normalized.equalsIgnoreCase(oNorm)) {
                occurrences++;
            }
        }
        if (occurrences >= 2) {
            repetitionScore += 30;
        }
        // Cross-source agreement
        if (pdfText != null && visualOcrText != null) {
            if (pdfText.toLowerCase().contains(lowerNorm) && visualOcrText.toLowerCase().contains(lowerNorm)) {
                repetitionScore += 30;
            }
        }

        // 12. Person-Name Grammar & Structure
        if (normalized.matches("^[A-Z][a-zA-Z.]*(?:\\s+[A-Z][a-zA-Z.]*)*$")) {
            long vowels = lowerNorm.chars().filter(ch -> "aeiouy".indexOf(ch) >= 0).count();
            double ratio = (double) vowels / Math.max(1, normalized.replace(" ", "").length());
            if (ratio >= 0.25 && ratio <= 0.60) {
                normalizationScore += 30;
            }
        }

        int totalScore = semanticScore + spatialScore + documentStructureScore + ocrQualityScore +
                repetitionScore + relationshipScore + normalizationScore;

        return new NameCandidate(
                raw,
                normalized,
                rc.sourceAnchor(),
                semanticScore,
                spatialScore,
                documentStructureScore,
                ocrQualityScore,
                repetitionScore,
                relationshipScore,
                normalizationScore,
                totalScore,
                disqualified,
                disqualificationReason
        );
    }

    /**
     * Cleans raw candidate text by removing labels, honorifics, and trailer clauses.
     */
    public String cleanRawCandidate(String raw) {
        if (raw == null) return "";
        String s = raw.trim()
                .replaceAll("(?i)^(?:This is to certify[.\\s]*(?:that)?|சான்றிதழ்[\\s:]*|சான்றிதழ் அளிக்கப்படுகிறது[\\s:]*|சான்றளிக்கப்படுகிறது[\\s:]*|வழங்கப்படுகிறது[\\s:]*|Certified that|This is certified that|It is certified that|hereby certify that|This certificate is issued to|issued in favou?r of|awarded to|यह प्रमाणित किया जाता है कि|प्रमाणित किया जाता है कि?|ధృవీకరించడమైనది|ದೃಢೀಕರಿಸಲಾಗಿದೆ)[\\s:]*", "")
                .replaceAll("(?i)^(?:Name of (?:the )?(?:Applicant|Certificate Holder|Person|Citizen|Beneficiary|Student|Candidate|Holder|Child|Ward)|Applicant Name|Beneficiary Name|Citizen Name|Holder Name|Card Holder|Certificate Holder|Person Name|Student Name|Candidate Name|Member Name|Individual Name|Child Name|Ward Name|" +
                        "விண்ணப்பதாரர் பெயர்|பயனாளி பெயர்|சான்றிதழ் வைத்திருப்பவர்|மாணவர் பெயர்|குழந்தை பெயர்|" +
                        "आवेदक का नाम|लाभार्थी का नाम|प्रमाणपत्र धारक|छात्र का नाम|नागरिक का नाम|" +
                        "దరఖాస్తుదారు పేరు|లబ్దిదారుని పేరు|విద్యార్థి పేరు|పౌరుని పేరు|" +
                        "ಅರ್ಜಿದಾರರ ಹೆಸರು|ಫಲಾನುಭವಿಯ ಹೆಸರು|ವಿದ್ಯಾರ್ಥಿಯ ಹೆಸರು|ನಾಗರಿಕರ ಹೆಸರು|" +
                        "Name|பெயர்|नाम|పేరు|ಹೆಸರು)[\\s:\\-]*", "")
                .replaceAll("(?i)^(?:Selvi|Thiru|Tmt|Shri|Sri|Smt|Mr|Mrs|Ms|Miss|Dr|Kum|Kumari|Master|செல்வி|திரு|திருமதி|ஸ்ரீ|श्री|श्रीमती|सुश्री|कुमारी)[.\\s]+", "")
                .replaceAll("(?i)^To\\s*[:\\-]?\\s*", "")
                .replaceAll("(?i)\\s+(?:daughter\\s+of|son\\s+of|wife\\s+of|husband\\s+of|child\\s+of|ward\\s+of|D/O|S/O|W/O|C/O|residing.*|is\\s+a\\s+resident.*|Late).*$", "")
                .replaceAll("(?i)\\s+(?:Income Certificate|Caste Certificate|Community Certificate|Certificate No|Total|Annual).*$", "")
                .replaceAll("(?i)\\b(?:daughter|son|wife|husband|Late|residing)\\b", "");

        // Repair common OCR punctuation artifacts inside name tokens
        // 1. OCR misread of 'iv' as 'v-' or 'v\'' (e.g. "Nv-etha" -> "Nivetha")
        s = s.replaceAll("(?i)\\b([b-df-hj-np-tv-z])v[-']([aeiou])", "$1iv$2");
        // 2. OCR intra-word hyphen/apostrophe between letters (e.g. "Ni-vetha" -> "Nivetha", "La-thika" -> "Lathika")
        s = s.replaceAll("(?i)\\b([A-Za-z]{2,})[-']([a-z]{2,})\b", "$1$2");
        // 3. OCR single letter hyphen prefix (e.g. "N-ivetha" -> "Nivetha", "L-athika" -> "Lathika")
        s = s.replaceAll("(?i)\\b([A-Za-z])[-']([a-z]{3,})\b", "$1$2");
        // 4. OCR single letter hyphen suffix (e.g. "Lathik-a" -> "Lathika", "Niveth-a" -> "Nivetha")
        s = s.replaceAll("(?i)\\b([A-Za-z]{3,})[-']([a-z])\b", "$1$2");
        // 5. OCR capitalized fragment hyphen (e.g. "Ni-Vetha" -> "NiVetha", "La-Thika" -> "LaThika")
        s = s.replaceAll("(?i)\\b([A-Za-z]{2,})[-']([A-Za-z]{2,})\b", "$1$2");
        // 6. OCR single initial letter hyphen (e.g. "N-Ivetha" -> "NIvetha", "L-Athika" -> "LAthika")
        s = s.replaceAll("(?i)\\b([A-Za-z])[-']([A-Za-z]{3,})\b", "$1$2");

        return s.replaceAll("[^A-Za-z\\s.]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalizes a person-name candidate by conservatively reconstructing OCR-fragmented words
     * (e.g. "L athika" -> "Lathika", "La thika" -> "Lathika", "Lat hika" -> "Lathika", "N ivetha" -> "Nivetha", "Niv etha" -> "Nivetha")
     * while strictly preserving legitimate multi-word names (e.g. "Lathika Kumar"),
     * and initials (e.g. "Lathika K", "K. Lathika", "K Lathika").
     */
    public String normalizePersonNameCandidate(String candidate) {
        if (candidate == null || candidate.isBlank()) return "";

        String s = candidate.replace('\u00A0', ' ')
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!s.contains(" ")) {
            return capitalizeWord(s);
        }

        List<String> tokens = new ArrayList<>(Arrays.asList(s.split(" ")));
        boolean changed = true;

        while (changed && tokens.size() > 1) {
            changed = false;
            for (int i = 0; i < tokens.size() - 1; i++) {
                String t1 = tokens.get(i);
                String t2 = tokens.get(i + 1);

                if (shouldMergeTokens(t1, t2)) {
                    String merged = mergeTokens(t1, t2);
                    tokens.set(i, merged);
                    tokens.remove(i + 1);
                    changed = true;
                    break;
                }
            }
        }

        // Capitalize each token appropriately
        for (int i = 0; i < tokens.size(); i++) {
            tokens.set(i, capitalizeWord(tokens.get(i)));
        }

        return String.join(" ", tokens);
    }

    private boolean shouldMergeTokens(String t1, String t2) {
        if (t1 == null || t2 == null || t1.isEmpty() || t2.isEmpty()) return false;

        String t1Alpha = t1.replaceAll("[^A-Za-z]", "");
        String t2Alpha = t2.replaceAll("[^A-Za-z]", "");
        if (t1Alpha.isEmpty() || t2Alpha.isEmpty()) return false;

        // Rule 1: Explicit initial with period (e.g. "K.", "M.") must NEVER be merged
        if (t1.endsWith(".") || t1.contains(".")) return false;

        // Rule 2: Trailing single uppercase letter initial (e.g. "Lathika K") must NEVER be merged
        if (t2.length() == 1 && Character.isUpperCase(t2.charAt(0))) return false;

        // Rule 3: Leading single uppercase letter initial followed by a Title Case name starting with a consonant
        // (e.g. "K Lathika") must NEVER be merged
        if (t1Alpha.length() == 1 && Character.isUpperCase(t1.charAt(0))
                && t2Alpha.length() >= 3 && Character.isUpperCase(t2.charAt(0))
                && !"AEIOUY".contains(t2Alpha.substring(0, 1).toUpperCase())) {
            return false;
        }

        // Rule 4: Two distinct Title-Case words of 3+ letters each (e.g. "Lathika Kumar", "Selvi Lathika", "Priya Sharma")
        // must NEVER be merged
        if (t1Alpha.length() >= 3 && Character.isUpperCase(t1.charAt(0))
                && t2Alpha.length() >= 3 && Character.isUpperCase(t2.charAt(0))
                && t2.length() > 1 && t2.substring(1).equals(t2.substring(1).toLowerCase())) {
            return false;
        }

        // Rule 5: Second token starts with lowercase letter (e.g. "L athika", "La thika", "Lat hika", "Lath ika", "Lathik a", "N ivetha", "Niv etha")
        // Standalone person-name words in Indian certificates are never lowercased.
        if (Character.isLowerCase(t2.charAt(0))) {
            String candidateMerged = t1Alpha + t2Alpha;
            return isValidMergedNameToken(candidateMerged);
        }

        // Rule 6: Single letter without period followed by capitalized token starting with vowel
        // where merged word is plausible (e.g. "L Athika" -> "LAthika" -> "Lathika", "N Ivetha" -> "Nivetha")
        if (t1Alpha.length() == 1 && Character.isUpperCase(t1.charAt(0))) {
            char firstOfT2 = Character.toUpperCase(t2.charAt(0));
            boolean t2StartsWithVowel = "AEIOUY".indexOf(firstOfT2) >= 0;
            if (t2StartsWithVowel && !"AEIOUY".contains(t1Alpha.toUpperCase())) {
                String candidateMerged = t1Alpha + t2Alpha;
                return isValidMergedNameToken(candidateMerged);
            }
        }

        // Rule 7: Short 2-letter fragment followed by Title Case word where merged word is a valid name
        // (e.g. "La Thika" -> "Lathika", "Ni Vetha" -> "Nivetha")
        if (t1Alpha.length() == 2 && Character.isUpperCase(t1.charAt(0))
                && t2Alpha.length() >= 3 && Character.isUpperCase(t2.charAt(0))) {
            String candidateMerged = t1Alpha + t2Alpha;
            if (isValidMergedNameToken(candidateMerged)) {
                return true;
            }
        }

        // Rule 8: Title Case word followed by 2-letter fragment
        // (e.g. "Niv Etha" -> "Nivetha", "Lat Hika" -> "Lathika")
        if (t1Alpha.length() >= 3 && Character.isUpperCase(t1.charAt(0))
                && t2Alpha.length() == 2 && Character.isUpperCase(t2.charAt(0))) {
            String candidateMerged = t1Alpha + t2Alpha;
            if (isValidMergedNameToken(candidateMerged)) {
                return true;
            }
        }

        // Rule 9: Title Case word of 4+ chars followed by single trailing uppercase letter that makes a valid word
        // (e.g. "Lathik A" -> "Lathika")
        if (t1Alpha.length() >= 4 && Character.isUpperCase(t1.charAt(0)) && t2Alpha.length() == 1) {
            String candidateMerged = t1Alpha + t2Alpha;
            if (isValidMergedNameToken(candidateMerged)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidMergedNameToken(String token) {
        if (token.length() < 3 || token.length() > 30) return false;
        String lower = token.toLowerCase();
        if (!lower.matches(".*[aeiouy].*")) return false;
        if (lower.matches(".*([a-z])\\1{2,}.*")) return false;
        if (GOVERNMENT_AUTHORITY_WORDS.contains(lower)) return false;
        if (LOCATION_WORDS.contains(lower)) return false;
        return true;
    }

    private String mergeTokens(String t1, String t2) {
        String combined = t1.replaceAll("[^A-Za-z]", "") + t2.replaceAll("[^A-Za-z]", "");
        if (combined.isEmpty()) return "";
        return Character.toUpperCase(combined.charAt(0)) + combined.substring(1).toLowerCase();
    }

    private String capitalizeWord(String w) {
        if (w == null || w.isEmpty()) return "";
        if (w.length() == 1 || (w.length() == 2 && w.endsWith("."))) {
            return w.toUpperCase();
        }
        return Character.toUpperCase(w.charAt(0)) + w.substring(1);
    }
}
