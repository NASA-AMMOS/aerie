package gov.nasa.jpl.ammos.mgss.aerie.simulation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nasa.jpl.ammos.mgss.aerie.simulation.model.CreateSimulationRequestBody;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessageData;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessageTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("simulation")
public class SimulationController {

    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

    @Value("${amqp.queue.name}")
    private String queueName;

    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;

    public SimulationController(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // TODO: Use a real schedule ID once the schedule service exists, for now, sub the planId
    // TODO: Begin scheduling when adaptation is loaded into runtime adaptation service
    @PostMapping
    public void createSimulation(@Valid @RequestBody CreateSimulationRequestBody createSimulation) {

        // TODO: Save this simulation to the database for future reference
        AmqpMessageData messageData = new AmqpMessageData();
        messageData.setAdditionalProperty("scheduleId", createSimulation.getScheduleId());
        messageData.setAdditionalProperty("adaptationId", createSimulation.getAdaptationId());
        messageData.setAdditionalProperty("simulationId", UUID.randomUUID().toString());

        AmqpMessage message = new AmqpMessage(AmqpMessageTypeEnum.LOAD_ADAPTATION, messageData);

        try {
            String jsonString = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(queueName, jsonString);
        } catch (JsonProcessingException e) {
            logger.error("Could not parse message", e);
        }

    }

}
