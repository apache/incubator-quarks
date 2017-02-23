package org.apache.edgent.connectors.iotp;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iotp.runtime.IotpGWConnector;
import org.apache.edgent.connectors.iotp.runtime.IotpGWDeviceEventsFixed;
import org.apache.edgent.connectors.iotp.runtime.IotpGWDeviceEventsFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;

class IotpGWDevice implements IotDevice {  // TODO implements IotpDevice ??? does GW allow for http?
  
  private final IotpGateway gateway;
  private final IotpGWConnector connector;
  private final Topology topology;
  private final String fqDeviceId;
  private final String deviceType;
  
  IotpGWDevice(IotpGateway gw, IotpGWConnector connector, Topology topology, String fqDeviceId) {
    this.gateway = gw;
    this.connector = connector;
    this.topology = topology;
    this.fqDeviceId = fqDeviceId;
    String[] devIdToks = IotpGWConnector.splitFqDeviceId(fqDeviceId);
    this.deviceType = devIdToks[0];
  }

  @Override
  public Topology topology() {
    return topology;
  }

  @Override
  public String getDeviceType() {
    return deviceType;
  }

  @Override
  public String getDeviceId() {
    return fqDeviceId;
  }

  @Override
  public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
      UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
    return stream.sink(
        new IotpGWDeviceEventsFunction(connector, jo -> fqDeviceId, eventId, payload, qos));
  }

  @Override
  public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {
    return stream.sink(new IotpGWDeviceEventsFixed(connector, fqDeviceId, eventId, qos));
  }

  @Override
  public TStream<JsonObject> commands(String... commands) {
    return gateway.commandsForDevice(fqDeviceId, commands);
  }
  
  @Override
  public boolean equals(Object o2) {
    return o2 == this 
        || equals(o2 instanceof IotpGWDevice && ((IotpGWDevice)o2).fqDeviceId.equals(fqDeviceId));
  }

  @Override
  public int hashCode() {
    return fqDeviceId.hashCode();
  }
  
  @Override
  public String toString() {
    return String.format("IotpGWDevice %s", fqDeviceId); 
  }
  
}