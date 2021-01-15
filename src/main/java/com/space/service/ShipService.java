package com.space.service;

import com.space.model.Ship;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ShipService {
    Ship getById(Long id);

    void save(Ship ship);

    void delete(Long id);

    Page<Ship> getAll(Specification<Ship> specification, Pageable pageable);

    Integer getCount(Specification<Ship> specification);

}
