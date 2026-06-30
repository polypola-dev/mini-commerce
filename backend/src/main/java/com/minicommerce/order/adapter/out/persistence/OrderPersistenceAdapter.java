package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.global.PageResult;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderStatus;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceAdapter implements OrderRepository {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "totalAmount", "status");

    private final JpaOrderRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(JpaOrderRepository jpaRepository, OrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(order)));
    }

    @Override
    public Optional<Order> findById(String id) {
        return jpaRepository.findByIdWithLines(id).map(mapper::toDomain);
    }

    @Override
    public List<Order> findAllByCustomerId(String customerId) {
        return jpaRepository.findAllByCustomerIdWithLines(customerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAllWithLines().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Order> findAllPaged(String status, String q, int page, int size, String sortBy, String sortDir) {
        String resolvedSort = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, resolvedSort);
        PageRequest pageable = PageRequest.of(page, size, sort);

        OrderStatus statusEnum = (status != null && !status.isBlank()) ? OrderStatus.valueOf(status) : null;
        String qParam = (q != null && !q.isBlank()) ? q : null;

        Page<String> idsPage = jpaRepository.findOrderIdsPaged(statusEnum, qParam, pageable);
        List<String> ids = idsPage.getContent();

        if (ids.isEmpty()) {
            return new PageResult<>(List.of(), idsPage.getTotalElements(), idsPage.getTotalPages(), page, size);
        }

        List<Order> orders = jpaRepository.findByIdsWithLines(ids).stream().map(mapper::toDomain).toList();
        Map<String, Order> byId = orders.stream().collect(Collectors.toMap(Order::getId, o -> o));
        List<Order> sorted = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

        return new PageResult<>(sorted, idsPage.getTotalElements(), idsPage.getTotalPages(), page, size);
    }
}
