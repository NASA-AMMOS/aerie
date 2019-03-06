package gov.nasa.jpl.aerie.activemq.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.Queue;

@RestController
public class Producer {

    @Autowired
    private Queue queue;

    @Autowired
    private JmsTemplate jmsTemplate;

    @GetMapping("publish/{msg}")
    public String publish(@PathVariable("msg") final String msg){
        jmsTemplate.convertAndSend(queue, msg);
        return String.format("Published message: %s", msg);
    }
}
