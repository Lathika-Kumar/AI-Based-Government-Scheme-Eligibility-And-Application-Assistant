package com.schemebridge.repository;

import com.schemebridge.entity.Role;
import com.schemebridge.enums.RoleEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByName(RoleEnum name);
    Boolean existsByName(RoleEnum name);
}
