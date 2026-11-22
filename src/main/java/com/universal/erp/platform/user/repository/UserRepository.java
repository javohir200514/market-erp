package com.universal.erp.platform.user.repository;

import com.universal.erp.platform.user.enums.UserStatus;
import com.universal.erp.platform.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByIdAndStatus(UUID id, UserStatus status);

    Page<User> findAllByStatus(UserStatus status, Pageable pageable);

    /** keyword null bo'lmasligi kerak: bo'sh qidiruv uchun "" bering. */
    @Query("""
            SELECT u
            FROM User u
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);

    /** keyword null bo'lmasligi kerak: bo'sh qidiruv uchun "" bering. */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.status = :status
              AND (
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<User> searchByStatus(@Param("keyword") String keyword,
                              @Param("status") UserStatus status,
                              Pageable pageable);
}