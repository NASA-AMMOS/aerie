
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * AmqpMessage
 * <p>
 * 
 * 
 */
public class AmqpMessage {

    /**
     * AmqpMessageTypeEnum
     * <p>
     * 
     * 
     */
    private AmqpMessageTypeEnum messageType;
    private AmqpMessageData data;

    /**
     * No args constructor for use in serialization
     * 
     */
    public AmqpMessage() {
    }

    /**
     * 
     * @param messageType
     * @param data
     */
    public AmqpMessage(AmqpMessageTypeEnum messageType, AmqpMessageData data) {
        super();
        this.messageType = messageType;
        this.data = data;
    }

    /**
     * AmqpMessageTypeEnum
     * <p>
     * 
     * 
     */
    public AmqpMessageTypeEnum getMessageType() {
        return messageType;
    }

    /**
     * AmqpMessageTypeEnum
     * <p>
     * 
     * 
     */
    public void setMessageType(AmqpMessageTypeEnum messageType) {
        this.messageType = messageType;
    }

    public AmqpMessageData getData() {
        return data;
    }

    public void setData(AmqpMessageData data) {
        this.data = data;
    }

}
