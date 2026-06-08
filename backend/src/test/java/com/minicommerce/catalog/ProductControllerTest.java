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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("GET /api/products (q 없음): findByActiveTrueOrderByNameAsc 호출 후 JSON 배열 반환")
    void listProducts_withoutQuery_callsFindByActiveTrue() throws Exception {
        // given
        Product product = new Product("p1", "사과", "새콤달콤한 과일", BigDecimal.valueOf(2000), 10, "img.jpg");
        when(productRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(product));
        when(inventoryService.availableStock("p1", 10L)).thenReturn(8L);
        when(productOptionRepository.findByProductId("p1")).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/products")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("p1"))
                .andExpect(jsonPath("$[0].name").value("사과"))
                .andExpect(jsonPath("$[0].description").value("새콤달콤한 과일"))
                .andExpect(jsonPath("$[0].price").value(2000))
                .andExpect(jsonPath("$[0].availableStock").value(8));

        // searchActive는 호출되면 안 됨
        verify(productRepository).findByActiveTrueOrderByNameAsc();
        verify(productRepository, never()).searchActive(any());
    }

    @Test
    @DisplayName("GET /api/products?q=검색어: searchActive(검색어) 호출 후 JSON 배열 반환")
    void listProducts_withQuery_callsSearchActive() throws Exception {
        // given
        String query = "검색어";
        Product product = new Product("p2", "검색어 상품", "검색어가 포함된 설명", BigDecimal.valueOf(5000), 3, "img2.jpg");
        when(productRepository.searchActive(query)).thenReturn(List.of(product));
        when(inventoryService.availableStock("p2", 3L)).thenReturn(3L);
        when(productOptionRepository.findByProductId("p2")).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("q", query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("p2"))
                .andExpect(jsonPath("$[0].name").value("검색어 상품"))
                .andExpect(jsonPath("$[0].description").value("검색어가 포함된 설명"))
                .andExpect(jsonPath("$[0].price").value(5000))
                .andExpect(jsonPath("$[0].availableStock").value(3));

        // findByActiveTrueOrderByNameAsc는 호출되면 안 됨
        verify(productRepository).searchActive(query);
        verify(productRepository, never()).findByActiveTrueOrderByNameAsc();
    }
}
