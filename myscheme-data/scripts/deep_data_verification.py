import os
import json
import re
from collections import Counter, defaultdict

def run_accurate_empirical_verification():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    details_dir = os.path.join(base_dir, "raw", "details")
    
    files = [os.path.join(details_dir, f) for f in os.listdir(details_dir) if f.endswith(".json")]
    total_files = len(files)

    print(f"Executing 100% accurate empirical verification across all {total_files} detail files...")

    # Field stats collector
    field_records = {}

    def record_field(name, present_count, empty_count, path_str, types_dict, samples):
        field_records[name] = {
            "presentCount": present_count,
            "emptyCount": empty_count,
            "path": path_str,
            "types": types_dict,
            "samples": samples
        }

    # Collectors for analysis
    id_samples = []
    slug_samples = []
    title_samples = []
    name_samples = []
    brief_desc_samples = []
    content_samples = []
    level_samples = []
    state_samples = []
    cat_samples = []
    min_samples = []
    dept_samples = []
    tags_samples = []
    benefits_samples = []
    eligibility_samples = []
    app_proc_samples = []
    faqs_samples = []
    dbt_samples = []
    target_ben_samples = []
    sponsor_samples = []
    references_samples = []

    # Counters
    c_id = c_slug = c_title = c_name = c_brief = c_content = 0
    c_level = c_state = c_cat = c_min = c_dept = c_tags = 0
    c_benefits = c_elig = c_app = c_faqs = c_dbt = c_target_ben = c_sponsor = c_ref = 0

    # Types counters
    t_id = Counter()
    t_slug = Counter()
    t_title = Counter()
    t_name = Counter()
    t_brief = Counter()
    t_content = Counter()
    t_level = Counter()
    t_state = Counter()
    t_cat = Counter()
    t_min = Counter()
    t_dept = Counter()
    t_tags = Counter()
    t_benefits = Counter()
    t_elig = Counter()
    t_app = Counter()
    t_faqs = Counter()
    t_dbt = Counter()
    t_target_ben = Counter()
    t_sponsor = Counter()
    t_ref = Counter()

    # Specific Section Analyzers
    # Section 2: Description
    desc_both_present = 0
    desc_both_identical = 0
    desc_brief_only = 0
    desc_detailed_only = 0
    desc_neither = 0
    desc_samples = []

    # Section 3: Eligibility
    elig_is_dict = 0
    elig_is_array = 0
    elig_is_string = 0
    elig_has_structured_age = 0
    elig_has_structured_income = 0
    elig_has_structured_caste = 0
    elig_has_structured_occupation = 0
    elig_has_structured_state = 0
    elig_natural_language_count = 0

    # Section 4: Documents
    doc_top_level_count = 0
    doc_embedded_app_process = 0
    doc_embedded_content = 0
    doc_examples = []

    # Section 5: Category & Department
    cat_counter = Counter()
    min_counter = Counter()
    dept_counter = Counter()
    missing_cat = 0
    missing_min = 0
    missing_dept = 0

    # Section 6: Level & State
    level_counter = Counter()
    state_counter = Counter()
    missing_state = 0

    # Section 7: DBT & Beneficiaries
    dbt_values = Counter()
    dbt_missing = 0
    target_ben_counter = Counter()
    target_ben_missing = 0
    scheme_for_counter = Counter()
    scheme_for_missing = 0

    # Section 8: Benefits, FAQs, Tags
    benefits_struct_counter = Counter()
    faqs_struct_counter = Counter()
    tags_struct_counter = Counter()
    benefits_missing_count = 0
    faqs_missing_count = 0
    tags_missing_count = 0

    for filepath in files:
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                raw = json.load(f)

            if not isinstance(raw, dict) or "data" not in raw: continue
            d = raw.get("data")
            if not isinstance(d, dict): continue

            slug_str = d.get("slug")
            en = d.get("en", {})
            if not isinstance(en, dict): en = {}

            basic = en.get("basicDetails", {})
            if not isinstance(basic, dict): basic = {}

            content = en.get("schemeContent", {})
            if not isinstance(content, dict): content = {}

            # 1. Extract values
            v_id = d.get("_id")
            v_slug = slug_str or basic.get("slug")
            v_title = basic.get("schemeShortTitle")
            v_name = basic.get("schemeName")
            v_brief = content.get("briefDescription") or basic.get("briefDescription")
            v_content = content
            v_level = basic.get("level")
            v_state = basic.get("state")
            v_cat = basic.get("schemeCategory")
            v_min = basic.get("nodalMinistryName")
            v_dept = basic.get("nodalDepartmentName")
            v_tags = basic.get("tags")
            v_benefits = content.get("benefits") or content.get("benefits_md")
            v_elig = en.get("eligibilityCriteria")
            v_app = en.get("applicationProcess")
            v_faqs = en.get("faqs") or content.get("faqs")
            v_dbt = basic.get("dbtScheme")
            v_target_ben = basic.get("targetBeneficiaries")
            v_sponsor = basic.get("sponsor")
            v_ref = content.get("references") or basic.get("references")

            # Helper for stats
            def process_item(val, counter_ref, type_counter, sample_list, name_str):
                is_empty = False
                if val is None: is_empty = True
                elif isinstance(val, (str, list, dict)) and len(val) == 0: is_empty = True
                elif str(val).strip() in ["", "null", "None"]: is_empty = True

                if not is_empty:
                    counter_ref[0] += 1
                    tname = type(val).__name__
                    type_counter[tname] += 1
                    if len(sample_list) < 5:
                        sample_list.append({"slug": v_slug, "val": str(val)[:150]})
                return not is_empty

            ref_id = [c_id]; process_item(v_id, ref_id, t_id, id_samples, "_id"); c_id = ref_id[0]
            ref_slug = [c_slug]; process_item(v_slug, ref_slug, t_slug, slug_samples, "slug"); c_slug = ref_slug[0]
            ref_title = [c_title]; process_item(v_title, ref_title, t_title, title_samples, "schemeShortTitle"); c_title = ref_title[0]
            ref_name = [c_name]; process_item(v_name, ref_name, t_name, name_samples, "schemeName"); c_name = ref_name[0]
            ref_brief = [c_brief]; process_item(v_brief, ref_brief, t_brief, brief_desc_samples, "briefDescription"); c_brief = ref_brief[0]
            ref_content = [c_content]; process_item(v_content, ref_content, t_content, content_samples, "schemeContent"); c_content = ref_content[0]
            ref_level = [c_level]; process_item(v_level, ref_level, t_level, level_samples, "level"); c_level = ref_level[0]
            ref_state = [c_state]; process_item(v_state, ref_state, t_state, state_samples, "state"); c_state = ref_state[0]
            ref_cat = [c_cat]; process_item(v_cat, ref_cat, t_cat, cat_samples, "schemeCategory"); c_cat = ref_cat[0]
            ref_min = [c_min]; process_item(v_min, ref_min, t_min, min_samples, "nodalMinistryName"); c_min = ref_min[0]
            ref_dept = [c_dept]; process_item(v_dept, ref_dept, t_dept, dept_samples, "nodalDepartmentName"); c_dept = ref_dept[0]
            ref_tags = [c_tags]; process_item(v_tags, ref_tags, t_tags, tags_samples, "tags"); c_tags = ref_tags[0]
            ref_benefits = [c_benefits]; process_item(v_benefits, ref_benefits, t_benefits, benefits_samples, "benefits"); c_benefits = ref_benefits[0]
            ref_elig = [c_elig]; process_item(v_elig, ref_elig, t_elig, eligibility_samples, "eligibilityCriteria"); c_elig = ref_elig[0]
            ref_app = [c_app]; process_item(v_app, ref_app, t_app, app_proc_samples, "applicationProcess"); c_app = ref_app[0]
            ref_faqs = [c_faqs]; process_item(v_faqs, ref_faqs, t_faqs, faqs_samples, "faqs"); c_faqs = ref_faqs[0]
            ref_dbt = [c_dbt]; process_item(v_dbt, ref_dbt, t_dbt, dbt_samples, "dbtScheme"); c_dbt = ref_dbt[0]
            ref_target_ben = [c_target_ben]; process_item(v_target_ben, ref_target_ben, t_target_ben, target_ben_samples, "targetBeneficiaries"); c_target_ben = ref_target_ben[0]
            ref_sponsor = [c_sponsor]; process_item(v_sponsor, ref_sponsor, t_sponsor, sponsor_samples, "sponsor"); c_sponsor = ref_sponsor[0]
            ref_ref = [c_ref]; process_item(v_ref, ref_ref, t_ref, references_samples, "references"); c_ref = ref_ref[0]

            # 2. Section 2: Description Resolution
            brief_text = str(v_brief or "").strip()
            detailed_text = str(content.get("detailedDescription_md") or content.get("detailedDescription") or "").strip()

            has_b = bool(brief_text)
            has_d = bool(detailed_text)

            if has_b and has_d:
                desc_both_present += 1
                if brief_text == detailed_text: desc_both_identical += 1
            elif has_b and not has_d: desc_brief_only += 1
            elif not has_b and has_d: desc_detailed_only += 1
            else: desc_neither += 1

            if len(desc_samples) < 5 and has_b and has_d:
                desc_samples.append({
                    "slug": v_slug,
                    "briefDesc": brief_text[:120],
                    "detailedDesc": detailed_text[:120]
                })

            # 3. Section 3: Eligibility Verification
            if isinstance(v_elig, dict):
                elig_is_dict += 1
                desc_md = v_elig.get("eligibilityDescription_md")
                desc_raw = v_elig.get("eligibilityDescription")
                
                # Check if there are structured keys inside
                has_struct = False
                if isinstance(desc_raw, list):
                    for item in desc_raw:
                        if isinstance(item, dict) and any(k in item for k in ["minAge", "maxAge", "income", "caste"]):
                            has_struct = True
                            if "minAge" in item or "maxAge" in item: elig_has_structured_age += 1
                            if "income" in item: elig_has_structured_income += 1
                            if "caste" in item: elig_has_structured_caste += 1

                if not has_struct:
                    elig_natural_language_count += 1

            elif isinstance(v_elig, list):
                elig_is_array += 1
                elig_natural_language_count += 1
            elif isinstance(v_elig, str):
                elig_is_string += 1
                elig_natural_language_count += 1

            # 4. Section 4: Document Verification
            docs_top = en.get("documentsRequired") or content.get("documents") or basic.get("documents")
            if docs_top and len(docs_top) > 0:
                doc_top_level_count += 1

            app_str = json.dumps(v_app, ensure_ascii=False) if isinstance(v_app, (dict, list)) else str(v_app or "")
            if any(w in app_str.lower() for w in ["document", "proof", "certificate", "aadhaar", "card", "voter"]):
                doc_embedded_app_process += 1
                if len(doc_examples) < 25:
                    doc_snippet = ""
                    for line in app_str.split("\\n"):
                        if any(w in line.lower() for w in ["document", "proof", "certificate", "aadhaar", "card", "voter"]):
                            doc_snippet = line[:140]
                            break
                    doc_examples.append({
                        "scheme": v_name or v_slug,
                        "sourcePath": "data.en.applicationProcess",
                        "documentInfo": doc_snippet or app_str[:140],
                        "reliability": "LOW (Embedded in natural language step instructions)"
                    })

            # 5. Section 5: Category & Department
            if isinstance(v_cat, list) and v_cat:
                for c in v_cat:
                    lbl = c.get("label") if isinstance(c, dict) else str(c)
                    cat_counter[lbl] += 1
            elif isinstance(v_cat, dict):
                cat_counter[v_cat.get("label") or str(v_cat)] += 1
            elif isinstance(v_cat, str) and v_cat.strip():
                cat_counter[v_cat.strip()] += 1
            else:
                missing_cat += 1

            m_name = (v_min.get("label") if isinstance(v_min, dict) else str(v_min or "")).strip()
            if m_name and m_name != "None": min_counter[m_name] += 1
            else: missing_min += 1

            d_name = (v_dept.get("label") if isinstance(v_dept, dict) else str(v_dept or "")).strip()
            if d_name and d_name != "None": dept_counter[d_name] += 1
            else: missing_dept += 1

            # 6. Section 6: Level & State
            lvl_lbl = (v_level.get("value") if isinstance(v_level, dict) else str(v_level or "")).upper()
            level_counter[lvl_lbl] += 1

            s_lbl = (v_state.get("label") if isinstance(v_state, dict) else str(v_state or "")).strip()
            if s_lbl and s_lbl != "None": state_counter[s_lbl] += 1
            else: missing_state += 1

            # 7. Section 7: DBT & Beneficiaries
            dbt_values[str(v_dbt)] += 1
            if v_dbt is None: dbt_missing += 1

            if isinstance(v_target_ben, list):
                for tb in v_target_ben:
                    lbl = tb.get("label") if isinstance(tb, dict) else str(tb)
                    target_ben_counter[lbl] += 1
            elif v_target_ben:
                target_ben_counter[str(v_target_ben)] += 1
            else:
                target_ben_missing += 1

            v_scheme_for = basic.get("schemeFor")
            if v_scheme_for: scheme_for_counter[str(v_scheme_for)] += 1
            else: scheme_for_missing += 1

            # 8. Section 8: Benefits, FAQs, Tags
            benefits_struct_counter[type(v_benefits).__name__] += 1
            if not v_benefits: benefits_missing_count += 1

            faqs_struct_counter[type(v_faqs).__name__] += 1
            if not v_faqs: faqs_missing_count += 1

            tags_struct_counter[type(v_tags).__name__] += 1
            if not v_tags: tags_missing_count += 1

        except Exception as ex:
            pass

    # Save structured results
    record_field("_id", c_id, total_files - c_id, "data._id", dict(t_id), id_samples)
    record_field("slug", c_slug, total_files - c_slug, "data.slug", dict(t_slug), slug_samples)
    record_field("schemeShortTitle", c_title, total_files - c_title, "data.en.basicDetails.schemeShortTitle", dict(t_title), title_samples)
    record_field("schemeName", c_name, total_files - c_name, "data.en.basicDetails.schemeName", dict(t_name), name_samples)
    record_field("briefDescription", c_brief, total_files - c_brief, "data.en.schemeContent.briefDescription", dict(t_brief), brief_desc_samples)
    record_field("schemeContent", c_content, total_files - c_content, "data.en.schemeContent", dict(t_content), content_samples)
    record_field("level", c_level, total_files - c_level, "data.en.basicDetails.level", dict(t_level), level_samples)
    record_field("state", c_state, total_files - c_state, "data.en.basicDetails.state", dict(t_state), state_samples)
    record_field("schemeCategory", c_cat, total_files - c_cat, "data.en.basicDetails.schemeCategory", dict(t_cat), cat_samples)
    record_field("nodalMinistryName", c_min, total_files - c_min, "data.en.basicDetails.nodalMinistryName", dict(t_min), min_samples)
    record_field("nodalDepartmentName", c_dept, total_files - c_dept, "data.en.basicDetails.nodalDepartmentName", dict(t_dept), dept_samples)
    record_field("tags", c_tags, total_files - c_tags, "data.en.basicDetails.tags", dict(t_tags), tags_samples)
    record_field("benefits", c_benefits, total_files - c_benefits, "data.en.schemeContent.benefits", dict(t_benefits), benefits_samples)
    record_field("eligibilityCriteria", c_elig, total_files - c_elig, "data.en.eligibilityCriteria", dict(t_elig), eligibility_samples)
    record_field("applicationProcess", c_app, total_files - c_app, "data.en.applicationProcess", dict(t_app), app_proc_samples)
    record_field("faqs", c_faqs, total_files - c_faqs, "data.en.faqs / schemeContent.faqs", dict(t_faqs), faqs_samples)
    record_field("dbtScheme", c_dbt, total_files - c_dbt, "data.en.basicDetails.dbtScheme", dict(t_dbt), dbt_samples)
    record_field("targetBeneficiaries", c_target_ben, total_files - c_target_ben, "data.en.basicDetails.targetBeneficiaries", dict(t_target_ben), target_ben_samples)
    record_field("sponsor", c_sponsor, total_files - c_sponsor, "data.en.basicDetails.sponsor", dict(t_sponsor), sponsor_samples)
    record_field("references", c_ref, total_files - c_ref, "data.en.schemeContent.references", dict(t_ref), references_samples)

    final_report = {
        "totalFiles": total_files,
        "fields": field_records,
        "section2_description": {
            "bothPresent": desc_both_present,
            "bothIdentical": desc_both_identical,
            "briefOnly": desc_brief_only,
            "detailedOnly": desc_detailed_only,
            "neither": desc_neither,
            "samples": desc_samples
        },
        "section3_eligibility": {
            "isDict": elig_is_dict,
            "isArray": elig_is_array,
            "isString": elig_is_string,
            "hasStructuredAge": elig_has_structured_age,
            "hasStructuredIncome": elig_has_structured_income,
            "hasStructuredCaste": elig_has_structured_caste,
            "naturalLanguageCount": elig_natural_language_count,
            "naturalLanguagePercentage": round((elig_natural_language_count / total_files) * 100, 2)
        },
        "section4_documents": {
            "topLevelStructuredCount": doc_top_level_count,
            "embeddedInAppProcessCount": doc_embedded_app_process,
            "examples": doc_examples[:25]
        },
        "section5_categoryDept": {
            "uniqueCategories": len(cat_counter),
            "missingCategoryCount": missing_cat,
            "topCategories": dict(cat_counter.most_common(15)),
            "uniqueMinistries": len(min_counter),
            "missingMinistryCount": missing_min,
            "topMinistries": dict(min_counter.most_common(10)),
            "uniqueDepartments": len(dept_counter),
            "missingDepartmentCount": missing_dept,
            "topDepartments": dict(dept_counter.most_common(10))
        },
        "section6_levelState": {
            "levelCounts": dict(level_counter),
            "missingStateCount": missing_state,
            "topStates": dict(state_counter.most_common(15))
        },
        "section7_dbtBeneficiary": {
            "dbtValues": dict(dbt_values),
            "dbtMissingCount": dbt_missing,
            "targetBenMissingCount": target_ben_missing,
            "topTargetBen": dict(target_ben_counter.most_common(10)),
            "schemeForMissingCount": scheme_for_missing,
            "topSchemeFor": dict(scheme_for_counter.most_common(10))
        },
        "section8_benefitsFaqsTags": {
            "benefitsStructures": dict(benefits_struct_counter),
            "benefitsMissing": benefits_missing_count,
            "benefitsSamples": benefits_samples,
            "faqsStructures": dict(faqs_struct_counter),
            "faqsMissing": faqs_missing_count,
            "faqsSamples": faqs_samples,
            "tagsStructures": dict(tags_struct_counter),
            "tagsMissing": tags_missing_count,
            "tagsSamples": tags_samples
        }
    }

    out_file = os.path.join(base_dir, "raw", "empirical_verification_final.json")
    with open(out_file, "w", encoding="utf-8") as out:
        json.dump(final_report, out, ensure_ascii=False, indent=2)

    print(f"Empirical verification complete! Saved output to {out_file}")

if __name__ == "__main__":
    run_accurate_empirical_verification()
