package com.instafit.core.repository;

import com.instafit.core.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByCityCode(String cityCode);

    boolean existsByCityCode(String cityCode);

    List<City> findByActive(Boolean active);

    List<City> findAllByOrderByCityCodeAsc();
}