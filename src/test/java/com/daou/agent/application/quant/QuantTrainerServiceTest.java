package com.daou.agent.application.quant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.daou.agent.application.port.MarketDataProvider;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuantTrainerServiceTest {

    @Test
    void shouldPredictFromAliasWithExternalSeries() {
        MarketDataProvider marketDataProvider = mock(MarketDataProvider.class);
        when(marketDataProvider.getDailyCloseSeries(anyString(), anyInt()))
                .thenReturn(upwardSeries(90, 70000.0, 0.0015));

        QuantTrainerService service = new QuantTrainerService(marketDataProvider, 90, 60);
        MarketPrediction prediction = service.predict("삼성전자", "", "", "week", List.of());

        assertThat(prediction.symbol()).isEqualTo("005930.KR");
        assertThat(prediction.assetType()).isEqualTo("stock");
        assertThat(prediction.horizon()).isEqualTo("1주일");
        assertThat(prediction.direction()).isEqualTo("상승");
        assertThat(prediction.confidence()).isBetween(0.35, 0.88);
        assertThat(prediction.evidence()).hasSize(3);
        assertThat(prediction.dataSource()).startsWith("stooq:");
        assertThat(prediction.dataPoints()).isGreaterThanOrEqualTo(20);
    }

    @Test
    void shouldFallbackWhenExternalSeriesFails() {
        MarketDataProvider marketDataProvider = mock(MarketDataProvider.class);
        when(marketDataProvider.getDailyCloseSeries(anyString(), anyInt()))
                .thenThrow(new IllegalStateException("network timeout"));

        QuantTrainerService service = new QuantTrainerService(marketDataProvider, 90, 60);
        MarketPrediction prediction = service.predict("코스피", "", "", "month", List.of());

        assertThat(prediction.symbol()).isEqualTo("KOSPI");
        assertThat(prediction.assetType()).isEqualTo("index");
        assertThat(prediction.horizon()).isEqualTo("1개월");
        assertThat(prediction.dataSource()).isEqualTo("fallback-simulated");
        assertThat(prediction.dataPoints()).isGreaterThanOrEqualTo(60);
        assertThat(prediction.rangeLow()).isGreaterThan(0);
        assertThat(prediction.rangeHigh()).isGreaterThan(prediction.rangeLow());
    }

    private List<MarketPricePoint> upwardSeries(int points, double start, double driftPerDay) {
        List<MarketPricePoint> list = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        double value = start;
        for (int i = 0; i < points; i++) {
            value = value * (1.0 + driftPerDay);
            list.add(new MarketPricePoint(startDate.plusDays(i), value));
        }
        return list;
    }
}
