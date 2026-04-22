package com.pongtorich.pong_to_rich.domain.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    Page<StockPrice> findByStockOrderByTradeDateDesc(Stock stock, Pageable pageable);

    List<StockPrice> findByStockOrderByTradeDateDesc(Stock stock);

    boolean existsByStockAndTradeDate(Stock stock, LocalDate tradeDate);
}
