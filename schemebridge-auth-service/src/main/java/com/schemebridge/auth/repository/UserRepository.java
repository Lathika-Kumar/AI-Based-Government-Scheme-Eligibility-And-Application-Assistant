package com.schemebridge.auth.repository;

import com.schemebridge.auth.entity.AccountStatus;
import com.schemebridge.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoles_NameAndAccountStatus(String roleName, AccountStatus status);

    @Query("SELECT count(DISTINCT u) FROM User u JOIN u.roles r WHERE UPPER(r.name) IN :roleNames")
    long countUsersByRoleNames(@Param("roleNames") Collection<String> roleNames);
}

