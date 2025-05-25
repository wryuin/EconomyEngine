package me.wryuin.messaging;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import me.wryuin.EconomyEngine;
import me.wryuin.utils.Messages;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class MessageBroker {
    private final EconomyEngine plugin;
    private final Connection connection;
    private final Channel channel;
    private final Gson gson;
    private static final String EXCHANGE_NAME = "economy_exchange";
    private static final String QUEUE_PREFIX = "economy_queue_";
    private static final String ROUTING_KEY = "economy.transaction";

    public MessageBroker(EconomyEngine plugin) throws IOException, TimeoutException {
        this.plugin = plugin;
        this.gson = new Gson();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(plugin.getConfig().getString("rabbitmq.host", "localhost"));
        factory.setPort(plugin.getConfig().getInt("rabbitmq.port", 5672));
        factory.setUsername(plugin.getConfig().getString("rabbitmq.username", "guest"));
        factory.setPassword(plugin.getConfig().getString("rabbitmq.password", "guest"));
        factory.setVirtualHost("/");

        this.connection = factory.newConnection();
        this.channel = connection.createChannel();

        // Declare exchange
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);

        // Declare queue with server-specific name
        String queueName = QUEUE_PREFIX + plugin.getServer().getPort();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, ROUTING_KEY);

        // Set up consumer
        channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                     AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                TransactionMessage txMessage = gson.fromJson(message, TransactionMessage.class);
                handleTransaction(txMessage);
            }
        });
    }

    public void publishTransaction(UUID from, UUID to, String currency, double amount) {
        TransactionMessage message = new TransactionMessage(from, to, currency, amount);
        try {
            String json = gson.toJson(message);
            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().severe(Messages.get("errors.rabbitmq.publish-failed", e.getMessage()));
        }
    }

    private void handleTransaction(TransactionMessage message) {
        // Execute on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getDatabase().transferBalance(
                    Bukkit.getOfflinePlayer(message.from()),
                    Bukkit.getOfflinePlayer(message.to()),
                    message.currency(),
                    message.amount()
            );
        });
    }

    public void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            plugin.getLogger().severe(Messages.get("errors.rabbitmq.connection-failed", e.getMessage()));
        }
    }

    private record TransactionMessage(UUID from, UUID to, String currency, double amount) {}
} 