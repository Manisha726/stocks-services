package com.example.demo;  // Current package

import org.springframework.boot.test.context.SpringBootTest;
import com.stocks.StockBackendApplication;  // Import your main application
import org.junit.jupiter.api.Test;


@SpringBootTest(classes = StockBackendApplication.class)
class DemoApplicationTests {
    
    @Test
    void contextLoads() {
    }
}