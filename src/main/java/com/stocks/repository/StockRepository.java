package com.stocks.repository;

import com.stocks.entity.Stock;
import com.stocks.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByUser(User user);
    Optional<Stock> findByUserAndSymbol(User user, String symbol);
}