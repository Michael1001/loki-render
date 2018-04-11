package net.whn.loki.messaging;

import net.whn.loki.error.MasterFrozenException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Provides a blocking queue for messages
 * NOTE: I can easily add other methods for timeout waits, etc if needed.
 */
public abstract class MsgQueue {

    private static final Logger log = Logger.getLogger(MsgQueue.class.toString());
    private final LinkedBlockingQueue<Message> messageQueue;

    public MsgQueue(int mqSize){
        messageQueue = new LinkedBlockingQueue<>(mqSize);
    }

    /**
     * blocks until it can place message on the queue
     * @param message
     * @throws InterruptedException if interrupted during block (shutdown)
     */
    public void deliverMessage(Message message) throws InterruptedException, MasterFrozenException{

        if(!messageQueue.offer(message, 10, TimeUnit.SECONDS)) {
            log.severe("failed to delivermessage w/ 10 second time out.");
            throw new MasterFrozenException(null);
        }
    }

    /**
     * blocks until there is a message to be taken.
     * this is ok since all actions by the master are initiated
     * via a message
     * @return
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public Message fetchNextMessage() throws InterruptedException {
        return messageQueue.take();
    }
}
