package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.concurrent.SynchronousQueue;

/**
 * Provides a means of synchronizing execution control between two threads
 * 
 * The `ControlChannel` is essentially a `SynchonousQueue` of size 0. Any thread that `put`s an element in the queue
 * blocks until another thread `take`s that element. Likewise, any thread that `take`s from the queue will block until
 * another thread `put`s an element in it.
 * 
 * This behavior is used to synchronize execution control between two threads. The element being passed back and forth
 * does not matter. Any thread may `yieldControl()`, blocking until another thread calls `takeControl()`. Successive
 * `yieldControl()` and `takeControl()` calls from within one thread essentially serve to relinquish control to another
 * thread (typically from the engine to an activity thread or vice versa) and then block until control is given back.
 * It is these successive calls that guarantee that execution control is seamlessly transferred from one thread to
 * another.
 */
public class ControlChannel {
    private final SynchronousQueue<Object> channel = new SynchronousQueue<>();

    /**
     * Yields execution control from the calling thread to another thread. Blocks until the other thread takes control.
     * 
     * See class-level docs for more information.
     */
    public void yieldControl() {
        try {
            this.channel.put(new Object());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Takes execution control from another thread to the calling thread. Blocks until the other thread yields control.
     * 
     * See class-level docs for more information.
     */
    public void takeControl() {
        try {
            this.channel.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}