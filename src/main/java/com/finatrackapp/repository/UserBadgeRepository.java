package com.finatrackapp.repository;

import com.finatrackapp.model.BadgeType;
import com.finatrackapp.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(Long userId);

    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
}
