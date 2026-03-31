package com.daou.agent.application.quant;

import com.daou.agent.application.port.MarketDataProvider;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QuantTrainerService {

    private static final int MIN_POINTS = 20;

    private static final Map<String, SymbolInfo> SYMBOL_ALIASES = Map.ofEntries(
            Map.entry("코스피", new SymbolInfo("코스피", "KOSPI", "kospi", "index")),
            Map.entry("kospi", new SymbolInfo("코스피", "KOSPI", "kospi", "index")),
            Map.entry("원달러", new SymbolInfo("원/달러", "USDKRW", "usdkrw", "fx")),
            Map.entry("환율", new SymbolInfo("원/달러", "USDKRW", "usdkrw", "fx")),
            Map.entry("usdkrw", new SymbolInfo("원/달러", "USDKRW", "usdkrw", "fx")),
            Map.entry("삼성전자", new SymbolInfo("삼성전자", "005930.KR", "005930.kr", "stock")),
            Map.entry("005930", new SymbolInfo("삼성전자", "005930.KR", "005930.kr", "stock")),
            Map.entry("sk하이닉스", new SymbolInfo("SK하이닉스", "000660.KR", "000660.kr", "stock")),
            Map.entry("하이닉스", new SymbolInfo("SK하이닉스", "000660.KR", "000660.kr", "stock")),
            Map.entry("000660", new SymbolInfo("SK하이닉스", "000660.KR", "000660.kr", "stock")),
            Map.entry("naver", new SymbolInfo("NAVER", "035420.KR", "035420.kr", "stock")),
            Map.entry("카카오", new SymbolInfo("카카오", "035720.KR", "035720.kr", "stock")),
            Map.entry("현대차", new SymbolInfo("현대차", "005380.KR", "005380.kr", "stock")),
            Map.entry("애플", new SymbolInfo("Apple", "AAPL.US", "aapl.us", "stock")),
            Map.entry("apple", new SymbolInfo("Apple", "AAPL.US", "aapl.us", "stock")),
            Map.entry("마이크로소프트", new SymbolInfo("Microsoft", "MSFT.US", "msft.us", "stock")),
            Map.entry("microsoft", new SymbolInfo("Microsoft", "MSFT.US", "msft.us", "stock")),
            Map.entry("엔비디아", new SymbolInfo("NVIDIA", "NVDA.US", "nvda.us", "stock")),
            Map.entry("nvidia", new SymbolInfo("NVIDIA", "NVDA.US", "nvda.us", "stock")),
            Map.entry("테슬라", new SymbolInfo("Tesla", "TSLA.US", "tsla.us", "stock")),
            Map.entry("tesla", new SymbolInfo("Tesla", "TSLA.US", "tsla.us", "stock")),
            Map.entry("아마존", new SymbolInfo("Amazon", "AMZN.US", "amzn.us", "stock")),
            Map.entry("amazon", new SymbolInfo("Amazon", "AMZN.US", "amzn.us", "stock")),
            Map.entry("메타", new SymbolInfo("Meta", "META.US", "meta.us", "stock")),
            Map.entry("meta", new SymbolInfo("Meta", "META.US", "meta.us", "stock"))
    );

    private final MarketDataProvider marketDataProvider;
    private final int lookbackDays;
    private final int fallbackPoints;

    public QuantTrainerService(
            MarketDataProvider marketDataProvider,
            @Value("${agent.quant.lookback-days:90}") int lookbackDays,
            @Value("${agent.quant.fallback-points:60}") int fallbackPoints
    ) {
        this.marketDataProvider = marketDataProvider;
        this.lookbackDays = Math.max(lookbackDays, MIN_POINTS);
        this.fallbackPoints = Math.max(fallbackPoints, MIN_POINTS);
    }

    public MarketPrediction predict(
            String target,
            String symbol,
            String assetType,
            String horizonToken,
            List<Double> recentPrices
    ) {
        ForecastHorizon horizon = ForecastHorizon.from(horizonToken);
        SymbolInfo resolved = resolveSymbol(target, symbol, assetType);
        PriceSeries priceSeries = loadSeries(resolved, recentPrices);
        List<Double> closes = priceSeries.points().stream()
                .map(MarketPricePoint::close)
                .toList();
        if (closes.size() < MIN_POINTS) {
            throw new IllegalArgumentException("예측에 필요한 데이터가 부족합니다. 최소 %d개 가격 데이터가 필요합니다.".formatted(MIN_POINTS));
        }

        double current = closes.get(closes.size() - 1);
        int shortWindow = Math.min(5, closes.size());
        int longWindow = Math.min(20, closes.size());
        int momentumWindow = Math.min(horizon.days(), closes.size() - 1);
        double shortMa = average(closes.subList(closes.size() - shortWindow, closes.size()));
        double longMa = average(closes.subList(closes.size() - longWindow, closes.size()));
        double momentum = closes.size() > momentumWindow
                ? (current / closes.get(closes.size() - 1 - momentumWindow)) - 1.0
                : 0.0;
        double maSpread = (shortMa / longMa) - 1.0;
        double expectedChange = clamp((momentum * 0.6) + (maSpread * 0.4), -0.18, 0.18);

        double dailyVolatility = calculateDailyVolatility(closes);
        double projectedVolatility = dailyVolatility * Math.sqrt(horizon.days());
        double expectedPrice = Math.max(0.01, current * (1.0 + expectedChange));
        double width = Math.max(current * 0.006, expectedPrice * projectedVolatility * 1.15);
        double rangeLow = Math.max(0.01, expectedPrice - width);
        double rangeHigh = expectedPrice + width;

        String direction = direction(expectedChange);
        double confidence = confidence(expectedChange, projectedVolatility);

        List<String> evidence = List.of(
                "최근 %d영업일 모멘텀 %.2f%%".formatted(momentumWindow, momentum * 100.0),
                "단기/중기 이동평균 괴리 %.2f%%".formatted(maSpread * 100.0),
                "예상 변동성(기간 환산) %.2f%%".formatted(projectedVolatility * 100.0)
        );

        return new MarketPrediction(
                resolved.displayName(),
                resolved.symbol(),
                resolved.assetType(),
                horizon.label(),
                direction,
                round(current, resolved.assetType()),
                round(expectedPrice, resolved.assetType()),
                round(rangeLow, resolved.assetType()),
                round(rangeHigh, resolved.assetType()),
                confidence,
                evidence,
                riskSummary(projectedVolatility),
                priceSeries.dataSource(),
                priceSeries.points().get(priceSeries.points().size() - 1).date().toString(),
                priceSeries.points().size()
        );
    }

    private PriceSeries loadSeries(SymbolInfo symbolInfo, List<Double> recentPrices) {
        List<Double> custom = sanitizePrices(recentPrices);
        if (custom.size() >= MIN_POINTS) {
            List<MarketPricePoint> points = buildPoints(custom, LocalDate.now().minusDays(custom.size()));
            return new PriceSeries(points, "user-input");
        }

        try {
            List<MarketPricePoint> points = marketDataProvider.getDailyCloseSeries(symbolInfo.providerSymbol(), lookbackDays);
            if (points.size() >= MIN_POINTS) {
                return new PriceSeries(points, "stooq:%s".formatted(symbolInfo.providerSymbol()));
            }
        } catch (Exception ignored) {
            // 외부 시세 실패 시 fallback 시계열로 예측을 지속한다.
        }

        List<MarketPricePoint> fallback = buildFallbackSeries(symbolInfo);
        return new PriceSeries(fallback, "fallback-simulated");
    }

    private List<MarketPricePoint> buildFallbackSeries(SymbolInfo symbolInfo) {
        double base = switch (symbolInfo.assetType()) {
            case "index" -> 2600.0;
            case "fx" -> 1350.0;
            default -> symbolInfo.symbol().endsWith(".KR") ? 80000.0 : 180.0;
        };

        Random random = new Random(symbolInfo.symbol().hashCode());
        List<Double> prices = new ArrayList<>();
        double value = base;
        for (int i = 0; i < fallbackPoints; i++) {
            double step = (random.nextDouble() - 0.48) * 0.022;
            value = Math.max(0.01, value * (1.0 + step));
            prices.add(value);
        }
        return buildPoints(prices, LocalDate.now().minusDays(fallbackPoints));
    }

    private SymbolInfo resolveSymbol(String target, String symbol, String assetType) {
        String keyFromTarget = normalize(target);
        if (!keyFromTarget.isBlank()) {
            SymbolInfo fromAlias = SYMBOL_ALIASES.get(keyFromTarget);
            if (fromAlias != null) {
                return fromAlias;
            }
        }

        String normalizedSymbol = symbol == null ? "" : symbol.trim();
        if (normalizedSymbol.isBlank()) {
            throw new IllegalArgumentException("quant.predict_market는 target 또는 symbol 인자가 필요합니다.");
        }

        String logical = normalizeLogicalSymbol(normalizedSymbol);
        String provider = toProviderSymbol(logical);
        String resolvedAssetType = normalizeAssetType(assetType, logical);
        String displayName = target == null || target.isBlank() ? logical : target.trim();
        return new SymbolInfo(displayName, logical, provider, resolvedAssetType);
    }

    private String normalizeLogicalSymbol(String symbol) {
        String trimmed = symbol.trim();
        if ("kospi".equalsIgnoreCase(trimmed)) {
            return "KOSPI";
        }
        if ("usdkrw".equalsIgnoreCase(trimmed)) {
            return "USDKRW";
        }
        if (trimmed.matches("\\d{6}")) {
            return trimmed + ".KR";
        }
        if (trimmed.contains(".")) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        if (trimmed.matches("[a-zA-Z]{1,5}")) {
            return trimmed.toUpperCase(Locale.ROOT) + ".US";
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String toProviderSymbol(String logicalSymbol) {
        if ("KOSPI".equalsIgnoreCase(logicalSymbol)) {
            return "kospi";
        }
        if ("USDKRW".equalsIgnoreCase(logicalSymbol)) {
            return "usdkrw";
        }
        return logicalSymbol.toLowerCase(Locale.ROOT);
    }

    private String normalizeAssetType(String assetType, String logicalSymbol) {
        String normalized = normalize(assetType);
        if ("index".equals(normalized) || "fx".equals(normalized) || "stock".equals(normalized)) {
            return normalized;
        }
        if ("KOSPI".equalsIgnoreCase(logicalSymbol)) {
            return "index";
        }
        if ("USDKRW".equalsIgnoreCase(logicalSymbol)) {
            return "fx";
        }
        return "stock";
    }

    private List<Double> sanitizePrices(List<Double> prices) {
        if (prices == null || prices.isEmpty()) {
            return List.of();
        }
        return prices.stream()
                .filter(value -> value != null && Double.isFinite(value) && value > 0)
                .collect(Collectors.toList());
    }

    private List<MarketPricePoint> buildPoints(List<Double> prices, LocalDate startDate) {
        List<MarketPricePoint> points = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            points.add(new MarketPricePoint(startDate.plusDays(i), prices.get(i)));
        }
        return points;
    }

    private double calculateDailyVolatility(List<Double> closes) {
        if (closes.size() < 3) {
            return 0.01;
        }
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < closes.size(); i++) {
            double previous = closes.get(i - 1);
            double current = closes.get(i);
            returns.add((current / previous) - 1.0);
        }
        int sampleWindow = Math.min(30, returns.size());
        List<Double> sampled = returns.subList(returns.size() - sampleWindow, returns.size());
        double mean = average(sampled);
        double variance = 0.0;
        for (double dailyReturn : sampled) {
            double gap = dailyReturn - mean;
            variance += gap * gap;
        }
        variance /= sampled.size();
        return Math.sqrt(variance);
    }

    private double confidence(double expectedChange, double projectedVolatility) {
        double signal = Math.abs(expectedChange);
        double ratio = signal / (projectedVolatility + 0.003);
        double value = 0.42 + Math.min(0.45, ratio * 0.22) - Math.min(0.18, projectedVolatility * 0.6);
        return clamp(value, 0.35, 0.88);
    }

    private String direction(double expectedChange) {
        if (expectedChange >= 0.007) {
            return "상승";
        }
        if (expectedChange <= -0.007) {
            return "하락";
        }
        return "횡보";
    }

    private String riskSummary(double projectedVolatility) {
        if (projectedVolatility >= 0.08) {
            return "변동성이 높은 구간입니다. 비중/손절 기준을 보수적으로 잡아야 합니다.";
        }
        if (projectedVolatility >= 0.04) {
            return "중간 변동성 구간입니다. 이벤트 리스크에 따라 방향이 빠르게 바뀔 수 있습니다.";
        }
        return "상대적으로 안정적이지만 단기 이벤트에 따른 변동 가능성은 남아 있습니다.";
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double round(double value, String assetType) {
        int scale = "fx".equals(assetType) ? 3 : 2;
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .trim();
    }

    private record SymbolInfo(
            String displayName,
            String symbol,
            String providerSymbol,
            String assetType
    ) {
    }

    private record PriceSeries(
            List<MarketPricePoint> points,
            String dataSource
    ) {
        private PriceSeries {
            points = points == null
                    ? List.of()
                    : points.stream()
                            .sorted(Comparator.comparing(MarketPricePoint::date))
                            .toList();
            dataSource = dataSource == null || dataSource.isBlank() ? "-" : dataSource;
        }
    }

    private enum ForecastHorizon {
        DAY("day", "다음날", 1),
        WEEK("week", "1주일", 5),
        MONTH("month", "1개월", 20);

        private final String key;
        private final String label;
        private final int days;

        ForecastHorizon(String key, String label, int days) {
            this.key = key;
            this.label = label;
            this.days = days;
        }

        public String label() {
            return label;
        }

        public int days() {
            return days;
        }

        public static ForecastHorizon from(String token) {
            if (token == null || token.isBlank()) {
                return DAY;
            }
            String normalized = token.toLowerCase(Locale.ROOT).replace(" ", "").trim();
            return switch (normalized) {
                case "day", "d1", "1d", "nextday", "내일", "다음날" -> DAY;
                case "week", "w1", "1w", "7d", "1주일", "이번주", "다음주" -> WEEK;
                case "month", "m1", "1m", "30d", "1개월", "이번달", "다음달" -> MONTH;
                default -> {
                    for (ForecastHorizon horizon : values()) {
                        if (horizon.key.equals(normalized)) {
                            yield horizon;
                        }
                    }
                    yield DAY;
                }
            };
        }
    }
}
