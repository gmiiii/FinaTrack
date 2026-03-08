package com.finatrackapp.service;

import com.finatrackapp.dto.request.CategoryRequest;
import com.finatrackapp.dto.response.CategoryResponse;
import com.finatrackapp.exception.DuplicateResourceException;
import com.finatrackapp.exception.InvalidOperationException;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.Category;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings("null")
public class CategoryService {

    private static final String CATEGORY_NOT_FOUND = "Kategori tidak ditemukan";

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    public List<CategoryResponse> getAllCategories(String email) {
        User user = userService.getUserByEmail(email);
        return categoryRepository.findAllAccessibleByUser(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Category category = findAccessibleCategory(id, user.getId());
        return toResponse(category);
    }

    public CategoryResponse createCategory(CategoryRequest request, String email) {
        User user = userService.getUserByEmail(email);

        if (categoryRepository.existsByNameAndUserIdAndType(
                request.getName(), user.getId(), request.getType())) {
            throw new DuplicateResourceException("Kategori dengan nama tersebut sudah ada");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setUser(user);
        categoryRepository.save(category);

        return toResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request, String email) {
        User user = userService.getUserByEmail(email);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));

        if (category.getUser() == null) {
            throw new InvalidOperationException("Kategori default tidak dapat diubah");
        }

        if (!category.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException(CATEGORY_NOT_FOUND);
        }

        category.setName(request.getName());
        category.setType(request.getType());
        categoryRepository.save(category);
        return toResponse(category);
    }

    public void deleteCategory(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));

        if (category.getUser() == null) {
            throw new InvalidOperationException("Kategori default tidak dapat dihapus");
        }

        if (!category.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException(CATEGORY_NOT_FOUND);
        }

        categoryRepository.delete(category);
    }

    private Category findAccessibleCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));

        boolean isSystemCategory = category.getUser() == null;
        boolean isOwnCategory = category.getUser() != null
                && category.getUser().getId().equals(userId);

        if (!isSystemCategory && !isOwnCategory) {
            throw new ResourceNotFoundException(CATEGORY_NOT_FOUND);
        }

        return category;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getUser() != null
        );
    }
}
