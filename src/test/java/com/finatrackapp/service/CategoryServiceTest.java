package com.finatrackapp.service;

import com.finatrackapp.dto.request.CategoryRequest;
import com.finatrackapp.dto.response.CategoryResponse;
import com.finatrackapp.exception.DuplicateResourceException;
import com.finatrackapp.exception.InvalidOperationException;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.Category;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;
    private Category systemCategory;
    private Category customCategory;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        systemCategory = new Category();
        systemCategory.setId(1L);
        systemCategory.setName("Makanan");
        systemCategory.setType(TransactionType.EXPENSE);
        systemCategory.setUser(null);

        customCategory = new Category();
        customCategory.setId(2L);
        customCategory.setName("Hobi");
        customCategory.setType(TransactionType.EXPENSE);
        customCategory.setUser(testUser);

        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Transportasi");
        categoryRequest.setType(TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("Get All - mengembalikan kategori sistem dan custom")
    void getAllCategories_ReturnsSystemAndCustom() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findAllAccessibleByUser(1L))
                .thenReturn(List.of(systemCategory, customCategory));

        List<CategoryResponse> result =
                categoryService.getAllCategories("test@example.com");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isCustom()).isFalse();
        assertThat(result.get(1).isCustom()).isTrue();
    }

    @Test
    @DisplayName("Get By ID - berhasil mengambil kategori yang accessible")
    void getCategoryById_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(systemCategory));

        CategoryResponse result =
                categoryService.getCategoryById(1L, "test@example.com");

        assertThat(result.name()).isEqualTo("Makanan");
    }

    @Test
    @DisplayName("Get By ID - gagal karena kategori tidak ditemukan")
    void getCategoryById_NotFound_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                categoryService.getCategoryById(99L, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get By ID - gagal akses kategori milik user lain")
    void getCategoryById_OtherUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        Category otherCategory = new Category();
        otherCategory.setId(3L);
        otherCategory.setUser(otherUser);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() ->
                categoryService.getCategoryById(3L, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Create - berhasil membuat kategori custom")
    void createCategory_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.existsByNameAndUserIdAndType(
                "Transportasi", 1L, TransactionType.EXPENSE)).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(i -> i.getArgument(0));

        CategoryResponse result =
                categoryService.createCategory(categoryRequest, "test@example.com");

        assertThat(result.name()).isEqualTo("Transportasi");
        assertThat(result.isCustom()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Create - gagal karena nama duplikat")
    void createCategory_Duplicate_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.existsByNameAndUserIdAndType(
                "Transportasi", 1L, TransactionType.EXPENSE)).thenReturn(true);

        assertThatThrownBy(() ->
                categoryService.createCategory(categoryRequest, "test@example.com"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update - berhasil mengupdate kategori custom")
    void updateCategory_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(customCategory));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(i -> i.getArgument(0));

        CategoryResponse result =
                categoryService.updateCategory(2L, categoryRequest, "test@example.com");

        assertThat(result.name()).isEqualTo("Transportasi");
    }

    @Test
    @DisplayName("Update - gagal mengubah kategori sistem/default")
    void updateCategory_SystemCategory_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() ->
                categoryService.updateCategory(1L, categoryRequest, "test@example.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Kategori default tidak dapat diubah");
    }

    @Test
    @DisplayName("Update - gagal mengubah kategori milik user lain")
    void updateCategory_OtherUserCategory_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        Category otherCategory = new Category();
        otherCategory.setId(3L);
        otherCategory.setUser(otherUser);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() ->
                categoryService.updateCategory(3L, categoryRequest, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Delete - berhasil menghapus kategori custom")
    void deleteCategory_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(customCategory));

        categoryService.deleteCategory(2L, "test@example.com");

        verify(categoryRepository).delete(customCategory);
    }

    @Test
    @DisplayName("Delete - gagal menghapus kategori sistem/default")
    void deleteCategory_SystemCategory_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() ->
                categoryService.deleteCategory(1L, "test@example.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Kategori default tidak dapat dihapus");
    }

    @Test
    @DisplayName("Delete - gagal menghapus kategori milik user lain")
    void deleteCategory_OtherUserCategory_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        Category otherCategory = new Category();
        otherCategory.setId(3L);
        otherCategory.setUser(otherUser);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() ->
                categoryService.deleteCategory(3L, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
