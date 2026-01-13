package com.instafit.core.repository;

import com.instafit.core.entity.OrderRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRouteRepository extends JpaRepository<OrderRoute, Long> {
    Optional<OrderRoute> findByCarpenterIdAndRouteDate(String carpenterId, LocalDate routeDate);
    List<OrderRoute> findByCarpenterId(String carpenterId);
    List<OrderRoute> findByRouteDate(LocalDate routeDate);
}