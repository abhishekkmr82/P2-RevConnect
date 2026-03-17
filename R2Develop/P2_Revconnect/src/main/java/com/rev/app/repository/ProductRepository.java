package com.rev.app.repository;

import com.rev.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
