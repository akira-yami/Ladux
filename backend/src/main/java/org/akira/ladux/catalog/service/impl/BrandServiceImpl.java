package org.akira.ladux.catalog.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.BrandRequest;
import org.akira.ladux.catalog.dto.response.BrandResponse;
import org.akira.ladux.catalog.entity.Brand;
import org.akira.ladux.catalog.mapper.BrandMapper;
import org.akira.ladux.catalog.repository.BrandRepository;
import org.akira.ladux.catalog.service.BrandService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.akira.ladux.shared.util.SlugUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository repo;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "brands", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public PageResponse<BrandResponse> getAllBrands(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(BrandMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "brands", key = "'id:' + #id")
    public BrandResponse getBrandById(int id) {
        return BrandMapper.fromEntity(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "brands", key = "'name:' + #name")
    public BrandResponse getBrandByName(String name) {
       Brand brand = repo.findByName(name)
               .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với tên = " + name));
       return BrandMapper.fromEntity(brand);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "brands", key = "'slug:' + #slug")
    public BrandResponse getBrandBySlug(String slug) {
      Brand brand = repo.findBySlug(slug)
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với slug = " + slug));
      return BrandMapper.fromEntity(brand);
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse createBrand(BrandRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }
        if (repo.existsByName(request.name())) {
            throw new IllegalArgumentException("Tên thương hiệu đã tồn tại");
        }
        String nameTrimmed = request.name().trim();
        String slug = SlugUtils.toSlug(nameTrimmed);
        Brand brand = Brand.builder()
                .name(nameTrimmed)
                .slug(slug)
                .logoUrl(request.logoUrl())
                .build();

        Brand savedBrand = repo.save(brand);

        return BrandMapper.fromEntity(savedBrand);
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse updateBrand(int id, BrandRequest brand) {
        if (brand.name() == null || brand.name().isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }
        Brand b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id = " + id));
        if (!b.getName().equalsIgnoreCase(brand.name()) && repo.existsByName(brand.name())) {
            throw new IllegalArgumentException("Tên thương hiệu đã tồn tại");
        }
        String nameTrimmed = brand.name().trim();
        b.setName(nameTrimmed);
        b.setSlug(SlugUtils.toSlug(nameTrimmed));
        b.setLogoUrl(brand.logoUrl());
        return BrandMapper.fromEntity(b);
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public void deleteBrandById(int id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy thương hiệu với id = " + id);
        }
        repo.deleteById(id);
    }

}
