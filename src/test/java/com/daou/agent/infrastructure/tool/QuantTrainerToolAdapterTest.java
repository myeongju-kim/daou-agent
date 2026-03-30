package com.daou.agent.infrastructure.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.daou.agent.application.quant.MarketPrediction;
import com.daou.agent.application.quant.QuantTrainerService;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuantTrainerToolAdapterTest {

    @Test
    void shouldReturnPredictionPayload() {
        QuantTrainerService quantTrainerService = mock(QuantTrainerService.class);
        QuantTrainerToolAdapter adapter = new QuantTrainerToolAdapter(quantTrainerService);

        when(quantTrainerService.predict(eq("엔비디아"), eq(""), eq(""), eq("day"), any()))
                .thenReturn(new MarketPrediction(
                        "NVIDIA",
                        "NVDA.US",
                        "stock",
                        "다음날",
                        "상승",
                        950.11,
                        962.40,
                        940.20,
                        972.30,
                        0.67,
                        List.of("근거1", "근거2"),
                        "변동성이 높은 구간입니다.",
                        "stooq:nvda.us",
                        "2026-03-31",
                        90
                ));

        ToolCallResult result = adapter.execute(new ToolCallRequest(
                "quant.predict_market",
                Map.of(
                        "target", "엔비디아",
                        "horizon", "day"
                )
        ));

        assertThat(result.status()).isEqualTo("ok");
        assertThat(result.rawData().get("symbol")).isEqualTo("NVDA.US");
        assertThat(result.rawData().get("direction")).isEqualTo("상승");
        assertThat(result.rawData().get("confidencePercent")).isEqualTo(67L);
        assertThat(result.rawData().get("range")).isInstanceOf(Map.class);
    }

    @Test
    void shouldThrowWhenTargetAndSymbolMissing() {
        QuantTrainerService quantTrainerService = mock(QuantTrainerService.class);
        QuantTrainerToolAdapter adapter = new QuantTrainerToolAdapter(quantTrainerService);

        when(quantTrainerService.predict(eq(""), eq(""), eq(""), eq("day"), any()))
                .thenThrow(new IllegalArgumentException("quant.predict_market는 target 또는 symbol 인자가 필요합니다."));

        assertThatThrownBy(() -> adapter.execute(new ToolCallRequest("quant.predict_market", Map.of())))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("target 또는 symbol");
    }
}
