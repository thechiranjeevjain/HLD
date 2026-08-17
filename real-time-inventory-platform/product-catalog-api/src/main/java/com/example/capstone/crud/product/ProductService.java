package com.example.capstone.crud.product;

import com.example.capstone.crud.error.DuplicateSkuException;
import com.example.capstone.crud.error.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String search, Boolean active, Pageable pageable) {
        return productRepository.findAll(filters(search, active), pageable)
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException(id));
    }

    public ProductResponse create(CreateProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = new Product(
                sku,
                request.name().trim(),
                normalizeOptional(request.description()),
                request.price(),
                request.currency(),
                request.stockQuantity(),
                request.active() == null || request.active()
        );

        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateSkuException(sku);
        }

        product.replaceWith(new UpdateProductRequest(
                sku,
                request.name().trim(),
                normalizeOptional(request.description()),
                request.price(),
                request.currency(),
                request.stockQuantity(),
                request.active()
        ));
        return ProductResponse.from(product);
    }

    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    private Specification<Product> filters(String search, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String contains = "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("sku")), contains),
                        builder.like(builder.lower(root.get("name")), contains)
                ));
            }

            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
