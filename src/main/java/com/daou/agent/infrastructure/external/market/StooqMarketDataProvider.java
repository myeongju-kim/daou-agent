package com.daou.agent.infrastructure.external.market;

import com.daou.agent.application.port.MarketDataProvider;
import com.daou.agent.application.quant.MarketPricePoint;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class StooqMarketDataProvider implements MarketDataProvider {

    private final RestClient restClient;

    public StooqMarketDataProvider(
            @Value("${agent.quant.market-data.base-url:https://stooq.com}") String baseUrl,
            @Value("${agent.quant.market-data.timeout-ms:4000}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public List<MarketPricePoint> getDailyCloseSeries(String symbol, int limit) {
        String csv = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/q/d/l/")
                        .queryParam("s", symbol)
                        .queryParam("i", "d")
                        .build())
                .retrieve()
                .body(String.class);

        if (csv == null || csv.isBlank()) {
            throw new IllegalStateException("시세 응답이 비어 있습니다: " + symbol);
        }

        List<MarketPricePoint> points = parseCsv(csv);
        if (points.isEmpty()) {
            throw new IllegalStateException("시세 데이터를 찾지 못했습니다: " + symbol);
        }

        int safeLimit = Math.max(1, limit);
        int fromIndex = Math.max(0, points.size() - safeLimit);
        return points.subList(fromIndex, points.size());
    }

    private List<MarketPricePoint> parseCsv(String csv) {
        String[] lines = csv.split("\\R");
        if (lines.length <= 1) {
            return List.of();
        }
        List<MarketPricePoint> points = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split(",");
            if (cells.length < 5) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(cells[0].trim());
                double close = Double.parseDouble(cells[4].trim());
                if (close > 0) {
                    points.add(new MarketPricePoint(date, close));
                }
            } catch (DateTimeParseException | NumberFormatException ignored) {
                // 파싱 불가 라인은 제외한다.
            }
        }
        return points.stream()
                .sorted(Comparator.comparing(MarketPricePoint::date))
                .toList();
    }
}
