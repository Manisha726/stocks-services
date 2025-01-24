package com.stocks.service;

import com.stocks.entity.Stock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocks.entity.User;
import com.stocks.repository.StockRepository;
import com.stocks.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;


@Service
public class StockService {

    @Value("${alpha.vantage.api.url}")
    private String apiUrl;

    @Value("${alpha.vantage.api.key}")
    private String apiKey;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UserRepository userRepository;

    // Method to add stock to the user's portfolio
    public Stock addStock(Stock stock, String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            stock.setUser(user); // Link stock to the user
            return stockRepository.save(stock);
        }
        throw new RuntimeException("User not found");
    }

    // Method to fetch all stocks associated with a user
    public List<Stock> getStocksByUser(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return stockRepository.findByUser(user);
        }
        throw new RuntimeException("User not found");
    }

    // Fetch real-time stock data from Alpha Vantage
    public ResponseEntity<Map<String, Object>> getRealTimeStockData(String ticker) {
        try {
            // Construct the API URL for both real-time and previous closing price
            String realTimeUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("function", "TIME_SERIES_INTRADAY")
                    .queryParam("symbol", ticker)
                    .queryParam("interval", "1min")
                    .queryParam("apikey", apiKey)
                    .toUriString();

            String dailyUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("function", "TIME_SERIES_DAILY")
                    .queryParam("symbol", ticker)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            // Create RestTemplate instance to make API calls
            RestTemplate restTemplate = new RestTemplate();

            // First, try fetching real-time data (1-minute interval)
            ResponseEntity<String> realTimeResponse = restTemplate.getForEntity(realTimeUrl, String.class);

            if (realTimeResponse.getStatusCode() == HttpStatus.OK) {
                // Parse the real-time data (the JSON response may vary based on the API)
                String realTimeData = realTimeResponse.getBody();

                // Parse the real-time data and get the most recent price
                String realTimePrice = extractRealTimePrice(realTimeData);

                // Get the stock name using SYMBOL_SEARCH API
                String stockName = getStockNameFromSymbol(ticker);

                // Create a map to return the data in JSON format
                Map<String, Object> response = new HashMap<>();
                response.put("name", stockName);
                response.put("price", realTimePrice);

                return ResponseEntity.ok(response);
            } else {
                // If real-time data fails, fall back to previous closing price
                ResponseEntity<String> dailyResponse = restTemplate.getForEntity(dailyUrl, String.class);

                if (dailyResponse.getStatusCode() == HttpStatus.OK) {
                    // Parse the daily data to extract the previous closing price
                    String dailyData = dailyResponse.getBody();

                    // Extract the last "close" price from the daily time series
                    String previousClosePrice = extractPreviousClosePrice(dailyData);

                    // Get the stock name using SYMBOL_SEARCH API
                    String stockName = getStockNameFromSymbol(ticker);

                    // Return in the JSON format as expected by the UI
                    Map<String, Object> response = new HashMap<>();
                    response.put("name", stockName);
                    response.put("price", previousClosePrice);

                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Error fetching stock data for " + ticker + " - " + dailyResponse.getStatusCode());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }
            }
        } catch (Exception e) {
            // Catch any exceptions (e.g., network errors, API issues)
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error fetching stock data for " + ticker);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Helper method to extract the real-time price from the intraday data
    private String extractRealTimePrice(String realTimeData) {
        try {
            // Parse the JSON data
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(realTimeData);
            JsonNode timeSeriesNode = rootNode.path("Time Series (1min)");

            // Extract the most recent timestamp and its closing price
            Iterator<Map.Entry<String, JsonNode>> fields = timeSeriesNode.fields();
            Map.Entry<String, JsonNode> latestEntry = null;
            while (fields.hasNext()) {
                latestEntry = fields.next();  // Get the last entry in the time series (latest data)
            }

            // Extract the real-time price (closing price for the most recent timestamp)
            if (latestEntry != null) {
                String realTimePrice = latestEntry.getValue().path("4. close").asText();
                return realTimePrice;
            } else {
                // Return default value of "1" if no real-time data is available
                return "1";
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Return default value of "1" in case of an error
            return "1";
        }
    }


    // Helper method to extract the previous closing price from the daily time series response
    private String extractPreviousClosePrice(String dailyData) {
        try {
            // Parse the JSON data
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(dailyData);
            JsonNode timeSeriesNode = rootNode.path("Time Series (Daily)");

            // Extract the last available day from the time series
            Iterator<Map.Entry<String, JsonNode>> fields = timeSeriesNode.fields();
            Map.Entry<String, JsonNode> lastEntry = null;
            while (fields.hasNext()) {
                lastEntry = fields.next();  // Get the last entry in the time series
            }

            // Extract the previous closing price
            if (lastEntry != null) {
                String previousClosePrice = lastEntry.getValue().path("4. close").asText();
                return previousClosePrice;
            } else {
                return "No previous closing price available.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing daily data";
        }
    }

    // public List<Map<String, String>> searchStocks(String keyword) {
    //     List<Map<String, String>> stockList = new ArrayList<>();
    //     try {
    //         // Construct API URL to search for stocks
    //         String searchUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
    //                 .queryParam("function", "SYMBOL_SEARCH")
    //                 .queryParam("keywords", keyword)
    //                 .queryParam("apikey", apiKey)
    //                 .toUriString();

    //         // Create RestTemplate instance to make the API call
    //         RestTemplate restTemplate = new RestTemplate();

    //         // Make GET request to Alpha Vantage API
    //         ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);

    //         // Check if the response is OK
    //         if (response.getStatusCode() == HttpStatus.OK) {
    //             // Parse the response to extract the stock results
    //             ObjectMapper objectMapper = new ObjectMapper();
    //             JsonNode rootNode = objectMapper.readTree(response.getBody());

    //             // Extract the list of matching stocks
    //             JsonNode bestMatchesNode = rootNode.path("bestMatches");

    //             if (bestMatchesNode.isArray()) {
    //                 for (JsonNode matchNode : bestMatchesNode) {
    //                     String symbol = matchNode.path("1. symbol").asText();
    //                     String name = matchNode.path("2. name").asText();

    //                     // Create a map for each stock with symbol and name
    //                     Map<String, String> stockInfo = new HashMap<>();
    //                     stockInfo.put("symbol", symbol);
    //                     stockInfo.put("name", name);

    //                     // Add map to the list
    //                     stockList.add(stockInfo);
    //                 }
    //             }
    //         } else {
    //             // Handle errors from the search API
    //             Map<String, String> errorMap = new HashMap<>();
    //             errorMap.put("error", "Error fetching search results for keyword: " + keyword);
    //             stockList.add(errorMap);
    //         }
    //     } catch (Exception e) {
    //         Map<String, String> errorMap = new HashMap<>();
    //         errorMap.put("error", "Error occurred while processing search for keyword: " + keyword);
    //         stockList.add(errorMap);
    //         e.printStackTrace();
    //     }

    //     return stockList;
    // }

    public List<Map<String, String>> searchStocks(String keyword) {
    List<Map<String, String>> stockList = new ArrayList<>();
    try {
        // Construct API URL to search for stocks
        String searchUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("function", "SYMBOL_SEARCH")
                .queryParam("keywords", keyword)
                .queryParam("apikey", apiKey)
                .toUriString();

        // Create RestTemplate instance to make the API call
        RestTemplate restTemplate = new RestTemplate();

        // Make GET request to Alpha Vantage API
        ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);

        // Check if the response is OK
        if (response.getStatusCode() == HttpStatus.OK) {
            // Parse the response to extract the stock results
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            // Extract the list of matching stocks
            JsonNode bestMatchesNode = rootNode.path("bestMatches");

            if (bestMatchesNode.isArray() && bestMatchesNode.size() > 0) {
                for (JsonNode matchNode : bestMatchesNode) {
                    String symbol = matchNode.path("1. symbol").asText();
                    String name = matchNode.path("2. name").asText();

                    // Create a map for each stock with symbol and name
                    Map<String, String> stockInfo = new HashMap<>();
                    stockInfo.put("symbol", symbol);
                    stockInfo.put("name", name);

                    // Add map to the list
                    stockList.add(stockInfo);
                }
            }
        }

        // If no results are found, provide static fallback data
        if (stockList.isEmpty()) {
            stockList = getStaticStockData();
        }
        } catch (Exception e) {
            stockList = getStaticStockData(); // Fallback to static data in case of error
            e.printStackTrace();
        }

        return stockList;
    }

    private List<Map<String, String>> getStaticStockData() {
        // Static fallback data for stocks
        List<Map<String, String>> staticData = new ArrayList<>();
        staticData.add(Map.of("symbol", "AAPL", "name", "Apple Inc."));
        staticData.add(Map.of("symbol", "MSFT", "name", "Microsoft Corporation"));
        staticData.add(Map.of("symbol", "GOOGL", "name", "Alphabet Inc."));
        staticData.add(Map.of("symbol", "AMZN", "name", "Amazon.com, Inc."));
        staticData.add(Map.of("symbol", "TSLA", "name", "Tesla, Inc."));
        return staticData;
    }



    // Inner class to represent stock symbol and name
    public static class StockInfo {
        private String symbol;
        private String name;

        public StockInfo(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private String getStockNameFromSymbol(String ticker) {
        try {
            String symbolSearchUrl = UriComponentsBuilder.fromHttpUrl("https://www.alphavantage.co/query")
                    .queryParam("function", "SYMBOL_SEARCH")
                    .queryParam("keywords", ticker)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(symbolSearchUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                // Parse the response to extract the name of the stock
                String responseBody = response.getBody();
                String stockName = extractStockNameFromSearchResponse(responseBody);
                return stockName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown Stock";  // Return default value if name cannot be fetched
    }

    private String extractStockNameFromSearchResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode bestMatchNode = rootNode.path("bestMatches");

            if (bestMatchNode.isArray() && bestMatchNode.size() > 0) {
                // Extract the first match's name (you can refine this based on your needs)
                JsonNode firstMatch = bestMatchNode.get(0);
                return firstMatch.path("2. name").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown Stock";  // Return default value if name cannot be parsed
    }
}
