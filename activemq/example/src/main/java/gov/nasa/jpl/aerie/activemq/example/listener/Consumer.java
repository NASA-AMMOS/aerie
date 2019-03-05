package gov.nasa.jpl.aerie.activemq.example.listener;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    @JmsListener(destination = "example-queue")
    public void listener(String msg){
        System.out.println("Received Message : "+msg);
    }
}


