package org.akira.ladux.catalog.repository;

import org.akira.ladux.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findByName(String name);

    Category findBySlug(String slug);

}

