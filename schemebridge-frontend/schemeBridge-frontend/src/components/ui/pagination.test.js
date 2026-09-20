import { describe, it, expect } from "vitest";

describe("Pagination Logic & Boundary Verification (BUG 2 Fix)", () => {
  const computePaginationRange = (currentPage, totalPages, itemsPerPage, totalItems) => {
    const total = Math.max(0, Number(totalItems) || 0);
    const safeTotalPages = Math.max(1, Number(totalPages) || 1);
    const safeCurrentPage = Math.min(Math.max(1, Number(currentPage) || 1), safeTotalPages);

    const startIndex = total === 0 ? 0 : Math.min((safeCurrentPage - 1) * itemsPerPage + 1, total);
    const endIndex = total === 0 ? 0 : Math.min(safeCurrentPage * itemsPerPage, total);

    return {
      safeCurrentPage,
      safeTotalPages,
      startIndex,
      endIndex,
      total,
      displayText: `Showing ${startIndex} to ${endIndex} of ${total} results`,
    };
  };

  const sliceSchemes = (schemes, currentPage, totalPages, itemsPerPage) => {
    const safePage = Math.min(Math.max(1, currentPage), totalPages);
    const startIndex = (safePage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, schemes.length);
    return schemes.slice(startIndex, endIndex);
  };

  const sample50Schemes = Array.from({ length: 50 }, (_, i) => ({ id: `scheme_${i + 1}`, name: `Scheme ${i + 1}` }));

  it("should correctly handle 10 per page on page 1", () => {
    const totalPages = Math.ceil(sample50Schemes.length / 10);
    const res = computePaginationRange(1, totalPages, 10, sample50Schemes.length);
    expect(res.startIndex).toBe(1);
    expect(res.endIndex).toBe(10);
    expect(res.total).toBe(50);
    expect(res.displayText).toBe("Showing 1 to 10 of 50 results");

    const sliced = sliceSchemes(sample50Schemes, 1, totalPages, 10);
    expect(sliced.length).toBe(10);
    expect(sliced[0].id).toBe("scheme_1");
    expect(sliced[9].id).toBe("scheme_10");
  });

  it("should correctly handle 20 per page", () => {
    const totalPages = Math.ceil(sample50Schemes.length / 20);
    const res = computePaginationRange(1, totalPages, 20, sample50Schemes.length);
    expect(res.startIndex).toBe(1);
    expect(res.endIndex).toBe(20);
    expect(res.total).toBe(50);
    expect(res.displayText).toBe("Showing 1 to 20 of 50 results");

    const page3Res = computePaginationRange(3, totalPages, 20, sample50Schemes.length);
    expect(page3Res.startIndex).toBe(41);
    expect(page3Res.endIndex).toBe(50);
    expect(page3Res.displayText).toBe("Showing 41 to 50 of 50 results");

    const slicedPage3 = sliceSchemes(sample50Schemes, 3, totalPages, 20);
    expect(slicedPage3.length).toBe(10);
    expect(slicedPage3[0].id).toBe("scheme_41");
    expect(slicedPage3[9].id).toBe("scheme_50");
  });

  it("should correctly handle 50 per page", () => {
    const totalPages = Math.ceil(sample50Schemes.length / 50);
    const res = computePaginationRange(1, totalPages, 50, sample50Schemes.length);
    expect(res.startIndex).toBe(1);
    expect(res.endIndex).toBe(50);
    expect(res.displayText).toBe("Showing 1 to 50 of 50 results");

    const sliced = sliceSchemes(sample50Schemes, 1, totalPages, 50);
    expect(sliced.length).toBe(50);
  });

  it("should never produce invalid range like 'Showing 61 to 50 of 50 results' when currentPage is stale", () => {
    // If currentPage was 7 (e.g. from a previous 10-per-page setting of 70 items), but totalItems became 50 (totalPages = 5)
    const stalePage = 7;
    const totalPages = Math.ceil(sample50Schemes.length / 10); // 5
    const res = computePaginationRange(stalePage, totalPages, 10, sample50Schemes.length);

    // Clamped safeCurrentPage should be 5
    expect(res.safeCurrentPage).toBe(5);
    expect(res.startIndex).toBe(41);
    expect(res.endIndex).toBe(50);
    expect(res.displayText).toBe("Showing 41 to 50 of 50 results");

    // Cards should NOT disappear
    const sliced = sliceSchemes(sample50Schemes, stalePage, totalPages, 10);
    expect(sliced.length).toBe(10);
    expect(sliced[0].id).toBe("scheme_41");
  });

  it("should correctly handle empty results", () => {
    const res = computePaginationRange(1, 1, 10, 0);
    expect(res.startIndex).toBe(0);
    expect(res.endIndex).toBe(0);
    expect(res.total).toBe(0);
    expect(res.displayText).toBe("Showing 0 to 0 of 0 results");

    const sliced = sliceSchemes([], 1, 1, 10);
    expect(sliced.length).toBe(0);
  });

  it("should handle last page with remainder correctly", () => {
    const sample25 = sample50Schemes.slice(0, 25);
    const totalPages = Math.ceil(sample25.length / 10); // 3 pages
    const res = computePaginationRange(3, totalPages, 10, sample25.length);
    expect(res.startIndex).toBe(21);
    expect(res.endIndex).toBe(25);
    expect(res.displayText).toBe("Showing 21 to 25 of 25 results");

    const sliced = sliceSchemes(sample25, 3, totalPages, 10);
    expect(sliced.length).toBe(5);
    expect(sliced[0].id).toBe("scheme_21");
    expect(sliced[4].id).toBe("scheme_25");
  });
});
