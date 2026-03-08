package com.finatrackapp.repository;

import com.finatrackapp.model.Category;
import com.finatrackapp.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId OR c.user IS NULL")
    List<Category> findAllAccessibleByUser(@Param("userId") Long userId);

    boolean existsByNameAndUserIdAndType(String name, Long userId, TransactionType type);
}
