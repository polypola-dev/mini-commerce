package com.minicommerce.catalog;

import com.minicommerce.inventory.InventoryService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// JwtVerificationFilter는 WebConfig에서 Servlet FilterRegistrationBean으로 등록되며
// /api/products 엔드포인트에는 적용되지 않는다.
// WebConfig는 @Value 프로퍼티(BFF_SECRET_KEY 등)가 필수이므로 @WebMvcTest 대신
// standaloneSetup으로 필터 없이 컨트롤러만 격리하여 테스트한다 (기존 JwtVerificationFilterTest 패턴 동일).
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private InventoryService inventoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productRepository, productOptionRepository, inventoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/products (q 없음): findWithFilters(active=true, q=null) 호출 후 페이지 응답 반환")
    void listProducts_withoutQuery_callsFindWithFilters() throws Exception {
        // given
        Product product = new Product("p1", "사과", "새콤달콤한 과일", BigDecimal.valueOf(2000), 10, "img.jpg");
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findWithFilters(eq(true), isNull(), any(Pageable.class))).thenReturn(page);
        when(inventoryService.availableStock("p1", 10L)).thenReturn(8L);
        when(productOptionRepository.findByProductId("p1")).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/products")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("p1"))
                .andExpect(jsonPath("$.content[0].name").value("사과"))
                .andExpect(jsonPath("$.content[0].description").value("새콤달콤한 과일"))
                .andExpect(jsonPath("$.content[0].price").value(2000))
                .andExpect(jsonPath("$.content[0].availableStock").value(8))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(productRepository).findWithFilters(eq(true), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/products?q=검색어: findWithFilters(active=true, q=검색어) 호출 후 페이지 응답 반환")
    void listProducts_withQuery_callsFindWithFilters() throws Exception {
        // given
        String query = "검색어";
        Product product = new Product("p2", "검색어 상품", "검색어가 포함된 설명", BigDecimal.valueOf(5000), 3, "img2.jpg");
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findWithFilters(eq(true), eq(query), any(Pageable.class))).thenReturn(page);
        when(inventoryService.availableStock("p2", 3L)).thenReturn(3L);
        when(productOptionRepository.findByProductId("p2")).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("q", query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("p2"))
                .andExpect(jsonPath("$.content[0].name").value("검색어 상품"))
                .andExpect(jsonPath("$.content[0].description").value("검색어가 포함된 설명"))
                .andExpect(jsonPath("$.content[0].price").value(5000))
                .andExpect(jsonPath("$.content[0].availableStock").value(3));

        verify(productRepository).findWithFilters(eq(true), eq(query), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/products?page=1&size=5: 지정한 page/size로 Pageable 생성")
    void listProducts_withPageAndSize_buildsCorrectPageable() throws Exception {
        // given
        Pageable pageable = PageRequest.of(1, 5, Sort.by("name").ascending());
        Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findWithFilters(eq(true), isNull(), any(Pageable.class))).thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(productRepository).findWithFilters(eq(true), isNull(), eq(pageable));
    }
}
