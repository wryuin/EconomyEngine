// TODO №.1: fix the bstats. Who know how to connect bstats normal make a PR pls
package me.wryuin.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.wryuin.EconomyEngine;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bstats.charts.SimplePie;

import java.util.concurrent.atomic.AtomicInteger;

public class MetricsManager {
    private final EconomyEngine plugin;
    private final MeterRegistry registry;
    private final Counter transactionsCounter;
    private final Timer transactionTimer;
    private final AtomicInteger activePlayersGauge;
    private final AtomicInteger totalBalanceGauge;

    public MetricsManager(EconomyEngine plugin) {
        this.plugin = plugin;
        this.registry = new SimpleMeterRegistry();
        
        // Initialize counters and timers first
        this.transactionsCounter = Counter.builder("economy.transactions")
                .description("Number of economic transactions")
                .register(registry);

        this.transactionTimer = Timer.builder("economy.transaction.duration")
                .description("Transaction execution time")
                .register(registry);

        this.activePlayersGauge = new AtomicInteger(0);
        this.totalBalanceGauge = new AtomicInteger(0);

        // Register gauges
        registry.gauge("economy.players.active", activePlayersGauge);
        registry.gauge("economy.balance.total", totalBalanceGauge);
        
        Metrics metrics = new Metrics(plugin, 25983);
        metrics.addCustomChart(new SingleLineChart("transactions", () -> 
            (int) transactionsCounter.count()));
        metrics.addCustomChart(new SimplePie("storage_type", () -> 
            plugin.getConfig().getString("storage.type", "mysql")));
    }

    public void recordTransaction() {
        transactionsCounter.increment();
    }

    public Timer.Sample startTransactionTimer() {
        return Timer.start(registry);
    }

    public void stopTransactionTimer(Timer.Sample sample) {
        sample.stop(transactionTimer);
    }

    public void updateActivePlayers(int count) {
        activePlayersGauge.set(count);
    }

    public void updateTotalBalance(int total) {
        totalBalanceGauge.set(total);
    }
} 
