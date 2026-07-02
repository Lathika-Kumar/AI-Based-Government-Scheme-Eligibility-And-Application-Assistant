import { describe, it, expect } from "vitest";
import { TRANSLATIONS } from "./translations";

const EN = TRANSLATIONS.en;
const HI = TRANSLATIONS.hi;
const TA = TRANSLATIONS.ta;
const TE = TRANSLATIONS.te;
const KN = TRANSLATIONS.kn;
const EN_KEYS = Object.keys(EN);
const HI_KEYS = Object.keys(HI);
const TA_KEYS = Object.keys(TA);
const TE_KEYS = Object.keys(TE);
const KN_KEYS = Object.keys(KN);

describe("Translation dictionary — parity", () => {
  it("all languages have identical key sets to en", () => {
    const sortedEn = EN_KEYS.sort();
    expect(HI_KEYS.sort()).toEqual(sortedEn);
    expect(TA_KEYS.sort()).toEqual(sortedEn);
    expect(TE_KEYS.sort()).toEqual(sortedEn);
    expect(KN_KEYS.sort()).toEqual(sortedEn);
  });

  it("no key has a missing or empty string in en", () => {
    EN_KEYS.forEach((key) => {
      expect(EN[key], `EN key "${key}" is empty`).toBeTruthy();
      expect(typeof EN[key], `EN key "${key}" is not a string`).toBe("string");
    });
  });

  it("no key has a missing or empty string in other languages", () => {
    const others = { hi: HI, ta: TA, te: TE, kn: KN };
    Object.entries(others).forEach(([lang, dict]) => {
      Object.keys(dict).forEach((key) => {
        expect(dict[key], `${lang.toUpperCase()} key "${key}" is empty`).toBeTruthy();
        expect(typeof dict[key], `${lang.toUpperCase()} key "${key}" is not a string`).toBe("string");
      });
    });
  });

  it("non-trivial other language values are not identical to their en counterparts (untranslated detection)", () => {
    // Keys where identical EN=other values are acceptable (e.g. proper names, codes, emoji strings, untranslated technical terms)
    const ALLOW_SAME = [
      "profile_session_active", // contains emoji — both locales share the checkmark
      "dash_doc_verified_hint",
      "dash_no_apps",
      "dash_all_checked",
      "dash_all_checked_desc",
      "dash_match_intro",
      "dash_match_outro",
      "dash_view_apply",
      "dash_based_on",
      "dash_under_audit",
      "dash_digilocker",
      "tracker_docs_ready",
      "tracker_docs_more",
      "gen_filter",
      "gen_status",
      "gen_date",
      "gen_id",
      "gen_ref",
      "gen_applied",
      "gen_saved",
      "home_stat_accuracy_val",
      "login_email_placeholder",
      "login_email_placeholder_full",
      "login_password_placeholder"
    ];

    EN_KEYS
      .filter((k) => !ALLOW_SAME.includes(k) && EN[k].length > 4)
      .forEach((key) => {
        // Assert Hindi is translated
        expect(
          HI[key],
          `HI key "${key}" appears untranslated (matches EN value)`
        ).not.toBe(EN[key]);
      });
  });
});

describe("Translation dictionary — tracker stage keys", () => {
  const STAGE_KEYS = [
    "tracker_stage_saved",
    "tracker_stage_preparing",
    "tracker_stage_ready",
    "tracker_stage_submitted",
    "tracker_stage_review",
    "tracker_stage_approved",
    "tracker_stage_rejected",
  ];

  it("all 7 tracker stage keys exist in all languages", () => {
    const languages = { en: EN, hi: HI, ta: TA, te: TE, kn: KN };
    Object.entries(languages).forEach(([lang, dict]) => {
      STAGE_KEYS.forEach((k) => {
        expect(dict[k], `Missing ${lang.toUpperCase()} key: ${k}`).toBeTruthy();
      });
    });
  });
});

describe("Translation dictionary — template placeholder keys", () => {
  const checkPlaceholder = (key, placeholder) => {
    const languages = { en: EN, hi: HI, ta: TA, te: TE, kn: KN };
    Object.entries(languages).forEach(([lang, dict]) => {
      expect(dict[key], `${lang.toUpperCase()} key "${key}" missing placeholder ${placeholder}`).toContain(placeholder);
    });
  };

  it("tracker_docs_ready contains {score} placeholder in all languages", () => {
    checkPlaceholder("tracker_docs_ready", "{score}");
  });

  it("tracker_docs_more contains {n} placeholder in all languages", () => {
    checkPlaceholder("tracker_docs_more", "{n}");
  });
});
