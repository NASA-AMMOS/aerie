package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.concurrent.SynchronousQueue;

public class ControlChannel {
    private final SynchronousQueue<Object> channel = new SynchronousQueue<>();

    public void yieldControl() {
        try {
            this.channel.put(new Object());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void takeControl() {
        try {
            this.channel.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}