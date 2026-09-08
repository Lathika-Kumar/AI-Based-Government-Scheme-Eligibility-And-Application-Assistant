package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.CategoryCreateRequest;
import com.schemebridge.scheme.dto.response.CategoryResponse;
import com.schemebridge.scheme.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/schemes/categories", "/api/categories"})
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Endpoints for managing Scheme categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a new category (Admin/Scheme Manager only)")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all categories (Public)")
    public ResponseEntity<List<CategoryResponse>> getAllCategories(@RequestParam(required = false) String status) {
        List<CategoryResponse> response = categoryService.getAllCategories(status);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get category by code (Public)")
    public ResponseEntity<CategoryResponse> getCategoryByCode(@PathVariable String code) {
        CategoryResponse response = categoryService.getCategoryByCode(code);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
