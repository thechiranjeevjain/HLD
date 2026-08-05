package com.example.capstone.crud.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.capstone.crud.error.DuplicateSkuException;
import com.example.capstone.crud.error.NotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createNormalizesSkuAndDefaultsActiveToTrue() {
        CreateProductRequest request = new CreateProductRequest(
                " sku-1001 ",
                " Keyboard ",
                " Compact layout ",
                new BigDecimal("129.99"),
                "USD",
                25,
                null
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-1001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.sku()).isEqualTo("SKU-1001");
        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.description()).isEqualTo("Compact layout");
        assertThat(response.active()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createRejectsDuplicateSku() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-1001",
                "Keyboard",
                null,
                new BigDecimal("129.99"),
                "USD",
                25,
                true
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-1001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-1001");
    }

    @Test
    void updateRejectsSkuOwnedByAnotherProduct() {
        UUID id = UUID.randomUUID();
        Product existing = new Product(
                "SKU-1001",
                "Keyboard",
                null,
                new BigDecimal("129.99"),
                "USD",
                25,
                true
        );
        UpdateProductRequest request = new UpdateProductRequest(
                "SKU-2002",
                "Keyboard Pro",
                null,
                new BigDecimal("149.99"),
                "USD",
                10,
                true
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-2002", id)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(id, request))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-2002");
    }

    @Test
    void deleteMissingProductThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
