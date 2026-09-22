package org.akira.ladux.catalog.repository;

import java.util.Optional;
import org.akira.ladux.catalog.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Integer> {

    Optional<Brand> findByName(String name);

    Optional<Brand> findBySlug(String slug);

    Boolean existsByName(String name);
}
