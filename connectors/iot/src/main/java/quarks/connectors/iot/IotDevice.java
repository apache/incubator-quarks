/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015,2016 
*/

package quarks.connectors.iot;

import com.google.gson.JsonObject;

import quarks.function.Function;
import quarks.function.UnaryOperator;
import quarks.topology.TSink;
import quarks.topology.TStream;
import quarks.topology.TopologyElement;

/**
 * Generic Internet of Things device connector.
 */
public interface IotDevice extends TopologyElement {

    /**
     * Publish a stream's tuples as device events.
     * <p>
     * Each tuple is published as a device event with the supplied functions
     * providing the event identifier, payload and QoS. The event identifier and
     * QoS can be generated based upon the tuple.
     * 
     * @param stream
     *            Stream to be published.
     * @param eventId
     *            function to supply the event identifier.
     * @param payload
     *            function to supply the event's payload.
     * @param qos
     *            function to supply the event's delivery Quality of Service.
     * @return TSink sink element representing termination of this stream.
     */
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
            UnaryOperator<JsonObject> payload,
            Function<JsonObject, Integer> qos) ;
    
    /**
     * Publish a stream's tuples as device events.
     * <p>
     * Each tuple is published as a device event with fixed event identifier and
     * QoS.
     * 
     * @param stream
     *            Stream to be published.
     * @param eventId
     *            Event identifier.
     * @param qos
     *            Event's delivery Quality of Service.
     * @return TSink sink element representing termination of this stream.
     */
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) ;

    /**
     * Create a stream of device commands as JSON objects.
     * Each command sent to the device matching {@code commands} will result in a tuple
     * on the stream. The JSON object has these keys:
     * <UL>
     * <LI>{@code command} - Command identifier as a String</LI>
     * <LI>{@code tsms} - Timestamp of the command in milliseconds since the 1970/1/1 epoch.</LI>
     * <LI>{@code format} - Format of the command as a String</LI>
     * <LI>{@code payload} - Payload of the command</LI>
     * <UL>
     * <LI>If {@code format} is {@code json} then {@code payload} is JSON</LI>
     * <LI>Otherwise {@code payload} is String
     * </UL>
     * </UL>
     * 
     * 
     * @param commands Command identifiers to include. If no command identifiers are provided then the
     * stream will contain all device commands.
     * @return Stream containing device commands.
     */
    public TStream<JsonObject> commands(String... commands);
}
