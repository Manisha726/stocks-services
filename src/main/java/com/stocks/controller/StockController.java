package com.stocks.controller;

import com.stocks.entity.Stock;
import com.stocks.service.StockService;
import com.stocks.repository.StockRepository;
import com.stocks.repository.UserRepository;
import com.stocks.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stocks.service.JwtService;
import com.stocks.exceptions.UnauthorizedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private static final String BEARER_PREFIX = "Bearer ";
    @Autowired
    private StockService stockService;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @PostMapping
    public ResponseEntity<Stock> addStock(@RequestBody Stock stock, @RequestHeader("Authorization") String token) {
        Long userId = jwtService.extractUserId(token.substring(7));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("Invalid token"));

        Optional<Stock> existingStock = stockRepository.findByUserAndSymbol(user, stock.getSymbol());
        if (existingStock.isPresent()) {
            Stock updated = existingStock.get();
            updated.setQuantity(updated.getQuantity() + stock.getQuantity());
            updated.setPrice(updated.getPrice() + stock.getPrice());
            return ResponseEntity.ok(stockRepository.save(updated));
        }

        stock.setUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(stockRepository.save(stock));
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getStocksByUser(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith(BEARER_PREFIX)) {
                throw new UnauthorizedException("Invalid authorization header format");
            }

            String jwtToken = token.substring(BEARER_PREFIX.length());
            Long userId = jwtService.extractUserId(jwtToken);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
            
            List<Stock> stocks = stockRepository.findByUser(user);
            return ResponseEntity.ok(stocks);
            
        } catch (UnauthorizedException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Invalid user ID in token");
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving stocks: " + e.getMessage(), e);
        }
    }

    @PutMapping("/sell")
    public ResponseEntity<Map<String, String>> sellStock(@RequestBody Map<String, Object> payload, @RequestHeader("Authorization") String token) {
        try {
            // Extract and validate the token
            if (token == null || !token.startsWith(BEARER_PREFIX)) {
                throw new UnauthorizedException("Invalid authorization header format");
            }
            String jwtToken = token.substring(BEARER_PREFIX.length());
            Long userId = jwtService.extractUserId(jwtToken);

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Extract payload
            String symbol = (String) payload.get("symbol");
            Integer quantityToSell = (Integer) payload.get("quantity");

            Optional<Stock> existingStockOpt = stockRepository.findByUserAndSymbol(user, symbol);
            if (existingStockOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("msg", "Stock not found"));
            }

            Stock existingStock = existingStockOpt.get();
            if (quantityToSell > existingStock.getQuantity()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("msg", "You have only " + existingStock.getQuantity() + " stocks of " + symbol));
            }

            if (quantityToSell.equals(existingStock.getQuantity())) {
                // If selling all stocks, remove the record
                stockRepository.delete(existingStock);
                return ResponseEntity.ok(Map.of("msg", "Sold all stocks of " + symbol));
            } else {
                // Update stock details
                double avgPricePerStock = existingStock.getPrice() / existingStock.getQuantity();
                existingStock.setQuantity(existingStock.getQuantity() - quantityToSell);
                existingStock.setPrice(existingStock.getPrice() - avgPricePerStock * quantityToSell);
                stockRepository.save(existingStock);
                return ResponseEntity.ok(Map.of("msg", "Sold " + quantityToSell + " stocks of " + symbol));
            }
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("msg", "Error processing the request: " + e.getMessage()));
        }
    }


    @GetMapping("/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeStockData(
            @RequestHeader("Authorization") String token,
            @RequestParam String ticker) {
        if (!token.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Invalid authorization header format");
        }
        jwtService.validateToken(token.substring(BEARER_PREFIX.length()));
        return stockService.getRealTimeStockData(ticker);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchStocks(
            @RequestHeader("Authorization") String token,
            @RequestParam String keyword) {
        if (!token.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Invalid authorization header format");
        }
        jwtService.validateToken(token.substring(BEARER_PREFIX.length()));
        try {
            return ResponseEntity.ok(stockService.searchStocks(keyword));
        } catch (Exception e) {
            throw new RuntimeException("Error searching stocks: " + e.getMessage(), e);
        }
    }
}
