package com.stocks.util;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class YahooFinanceClient {

    public JSONObject getStockData(String ticker) {
        // Mock JSON response (Replace this with actual Yahoo Finance API integration)
        JSONObject stockData = new JSONObject();
        stockData.put("symbol", ticker);
        stockData.put("price", 123.45);
        return stockData;
    }
}
