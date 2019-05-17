package gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AdaptationRuntimeApplication {

  @Value("${amqp.queue.name}")
  private String queueName;

  @Value("${amqp.exchange.name}")
  private String exchangeName;

  @Bean
  public Queue queue() {
    return new Queue(queueName, false);
  }

  @Bean
  public TopicExchange exchange() {
    return new TopicExchange(exchangeName);
  }

  @Bean
  public Binding binding(Queue queue, TopicExchange topicExchange) {
    return BindingBuilder.bind(queue).to(topicExchange).with(queueName);
  }

  @Bean
  public MessageListenerAdapter listenerAdapter(MessageReceiver handler) {
    return new MessageListenerAdapter(handler, "receiveMessage");
  }

  @Bean
  SimpleMessageListenerContainer container(
      ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(queueName);
    container.setMessageListener(listenerAdapter);
    return container;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  public static void main(String[] args) {
    SpringApplication.run(AdaptationRuntimeApplication.class, args);
  }
}
