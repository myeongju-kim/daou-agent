package com.daou.agent.application.port;

import com.daou.agent.application.quant.MarketPricePoint;
import java.util.List;

public interface MarketDataProvider {

    List<MarketPricePoint> getDailyCloseSeries(String symbol, int limit);
}
