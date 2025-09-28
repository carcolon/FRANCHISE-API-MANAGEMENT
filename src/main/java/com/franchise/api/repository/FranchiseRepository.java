package com.franchise.api.repository;

import com.franchise.api.domain.Franchise;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FranchiseRepository extends MongoRepository<Franchise, String> {

    Optional<Franchise> findByNameIgnoreCase(String name);
}
