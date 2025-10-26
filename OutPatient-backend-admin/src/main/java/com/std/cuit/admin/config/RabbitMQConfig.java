package com.std.cuit.admin.config;

import com.std.cuit.common.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /**
     * 死信队列配置
     */
    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead.letter.routing.key";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        // 配置重试机制
        rabbitTemplate.setRetryTemplate(new org.springframework.retry.support.RetryTemplate());

        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    /**
     * 声明反馈消息交换机 - 使用 Constants 中定义的名称
     */
    @Bean
    public TopicExchange feedbackMessageExchange() {
        return new TopicExchange(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE); // "hros.topic"
    }

    /**
     * 声明通用反馈消息队列
     */
    @Bean
    public Queue feedbackMessageQueue() {
        return QueueBuilder.durable(Constants.MessageKey.FEEDBACK_QUEUE_NAME) // "feedback.message.queue"
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .withArgument("x-message-ttl", 30L * 24 * 60 * 60 * 1000) // 30天过期
                .withArgument("x-queue-mode", "lazy") // 延迟队列模式
                .build();
    }

    /**
     * 绑定通用反馈消息队列到交换机
     */
    @Bean
    public Binding feedbackMessageBinding() {
        return BindingBuilder.bind(feedbackMessageQueue())
                .to(feedbackMessageExchange())
                .with("#"); // 使用通配符路由键，匹配所有路由键
    }

    /**
     * 声明死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    /**
     * 声明死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    /**
     * 绑定死信队列
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * 声明用户消息广播队列（可选，用于广播消息）
     */
    @Bean
    public FanoutExchange userBroadcastExchange() {
        return new FanoutExchange("user.broadcast.exchange");
    }

    /**
     * 声明用户广播队列
     */
    @Bean
    public Queue userBroadcastQueue() {
        return QueueBuilder.durable("user.broadcast.queue")
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定用户广播队列
     */
    @Bean
    public Binding userBroadcastBinding() {
        return BindingBuilder.bind(userBroadcastQueue())
                .to(userBroadcastExchange());
    }
}