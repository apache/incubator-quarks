/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.edgent.connectors.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.topology.json.JsonFunctions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.JsonObject;

/**
 * MQTT broker connector configuration.
 */
public class MqttConfig {
    private MqttConnectOptions options = new MqttConnectOptions();
    private String clientId;
    private MqttClientPersistence persistence;
    private long actionTimeToWaitMillis = -1;
    private int idleTimeout;
    private int subscriberIdleReconnectIntervalSec = 60;
    
    /**
     * Create a new configuration from {@link Properties}.
     * <p>
     * There is a property corresponding to each {@code MqttConfig.set<name>()}
     * method.  Unless otherwise stated, the property's value is a string
     * of the corresponding method's argument type.
     * Properties not specified yield a configuration value as
     * described by and their corresponding {@code set<name>()}.
     * <p>
     * Properties other than those noted are ignored.
     * 
     * <h3>MQTT connector properties</h3>
     * <ul>
     * <li>mqtt.actionTimeToWaitMillis</li>
     * <li>mqtt.cleanSession</li>
     * <li>mqtt.clientId</li>
     * <li>mqtt.connectionTimeoutSec</li>
     * <li>mqtt.idleTimeoutSec</li>
     * <li>mqtt.keepAliveSec</li>
     * <li>mqtt.keyStore - optional. Only used with "ssl:" serverURL when the
     *     server is configured for client auth.
     *     Path to key store file in JKS format.
     *     The first key in the store is used.  The key must have the same
     *     password as the store if any.
     *     If not set, the standard JRE and javax.net.ssl system properties
     *     control the SSL behavior.</li>
     * <li>mqtt.keyStorePassword - required if mqtt.keyStore is set.</li>
     * <li>mqtt.password</li>
     * <li>mqtt.persistence</li>
     * <li>mqtt.serverURLs - csv list of MQTT URLs of the form: 
     *                          {@code tcp://<host>:<port>} or
     *                          {@code ssl://<host>:<port>}
     *    </li>
     * <li>mqtt.subscriberIdleReconnectIntervalSec</li>
     * <li>mqtt.trustStore - optional. Only used with "ssl:" serverURL.
     *     Path to trust store file in JKS format.
     *     If not set, the standard JRE and javax.net.ssl system properties
     *     control the SSL behavior.
     *     Generally not required if server has a CA-signed certificate.</li>
     * <li>mqtt.trustStorePassword - required if mqtt.trustStore is set</li>
     * <li>mqtt.userName</li>
     * <li>mqtt.will - JSON for with the following properties:
     *     <ul>
     *     <li>topic - string</li>
     *     <li>payload - string for byte[] in UTF8</li>
     *     <li>qos - integer</li>
     *     <li>retained - boolean</li>
     *     </ul>
     *     </li>
     * </ul>
     * @param properties  properties specifying the configuration. 
     *        
     * @return the configuration
     * @throws IllegalArgumentException for illegal values
     */
    public static MqttConfig fromProperties(Properties properties) {
        MqttConfig config = new MqttConfig();
        Properties p = properties;
        setConfig(p, "mqtt.actionTimeToWaitMillis", 
                val -> config.setActionTimeToWaitMillis(Long.valueOf(val)));
        setConfig(p, "mqtt.cleanSession", 
                val -> config.setCleanSession(Boolean.valueOf(val)));
        setConfig(p, "mqtt.clientId", 
                val -> config.setClientId(val));
        setConfig(p, "mqtt.connectionTimeoutSec", 
                val -> config.setConnectionTimeout(Integer.valueOf(val)));
        setConfig(p, "mqtt.idleTimeoutSec", 
                val -> config.setIdleTimeout(Integer.valueOf(val)));
        setConfig(p, "mqtt.keepAliveSec", 
                val -> config.setKeepAliveInterval(Integer.valueOf(val)));
        setConfig(p, "mqtt.keyStore", 
                val -> config.setKeyStore(val));
        setConfig(p, "mqtt.keyStorePassword", 
                val -> config.setKeyStorePassword(val.toCharArray()));
// paho MqttConnectOptions.setSslProperties() doesn't support this control
//        setConfig(p, "mqtt.keyPassword", 
//                val -> config.setKeyPassword(val.toCharArray()));
//        setConfig(p, "mqtt.keyCertificateAlias", 
//                val -> config.setKeyCertificateAlias(val));
        setConfig(p, "mqtt.password", 
                val -> config.setPassword(val.toCharArray()));
        setConfig(p, "mqtt.persistence", 
                val -> config.setPersistence(newPersistenceProvider(val)));
        setConfig(p, "mqtt.serverURLs", 
                val -> config.setServerURLs(val.trim().split(",")));
        setConfig(p, "mqtt.subscriberIdleReconnectIntervalSec", 
                val -> config.setSubscriberIdleReconnectInterval(Integer.valueOf(val)));
        setConfig(p, "mqtt.trustStore", 
                val -> config.setTrustStore(val));
        setConfig(p, "mqtt.trustStorePassword", 
                val -> config.setTrustStorePassword(val.toCharArray()));
        setConfig(p, "mqtt.userName", 
                val -> config.setUserName(val));
        setConfig(p, "mqtt.will", val -> {
                        JsonObject jo = JsonFunctions.fromString().apply(val);
                        String topic = jo.get("topic").getAsString();
                        byte[] payload = jo.get("payload").getAsString().getBytes(StandardCharsets.UTF_8);
                        int qos = jo.get("qos").getAsInt();
                        boolean retained = jo.get("retained").getAsBoolean();
                        config.setWill(topic, payload, qos, retained);      
                    });
        return config;
    }

    private static void setConfig(Properties p, String name, Consumer<String> setter) {
        try {
            String value = p.getProperty(name);
            if (value != null) {
                setter.accept(value);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(name, e);
        }
    }
    
    private static MqttClientPersistence newPersistenceProvider(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
            return (MqttClientPersistence) clazz.newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Create a new configuration.
     */
    public MqttConfig() { }
        
    /**
     * Create a new configuration.
     * @param serverURL the MQTT broker's URL
     * @param clientId the MQTT client's id.  Auto-generated if null.
     */
    public MqttConfig(String serverURL, String clientId) {
        options.setServerURIs(new String[] {serverURL});
        this.clientId = clientId;
    }
  
    /**
     * Get the connection Client Id.
     * @return the value
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Get the maximum time to wait for an action (e.g., publish message) to complete.
     * @return the value
     */
    public long getActionTimeToWaitMillis() {
        return actionTimeToWaitMillis;
    }
    
    /**
     * Get the QoS 1 and 2 in-flight message persistence handler.
     * @return the value
     */
    public MqttClientPersistence getPersistence() {
        return persistence;
    }
    
    /**
     * Get the connection timeout.
     * @return the value
     */
    public int getConnectionTimeout() {
        return options.getConnectionTimeout();
    }
    
    /**
     * Get the idle connection timeout.
     * @return the value
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Get the subscriber idle reconnect interval.
     * @return the value
     */
    public int getSubscriberIdleReconnectInterval() {
        return subscriberIdleReconnectIntervalSec;
    }

    /**
     * Get the connection Keep alive interval.
     * @return the value
     */
    public int getKeepAliveInterval() {
        return options.getKeepAliveInterval();
    }

    /**
     * Get the MQTT Server URLs
     * @return the value
     */
    public String[] getServerURLs() {
        return options.getServerURIs();
    }

    /**
     * Get a Last Will and Testament message's destination topic.
     * @return the value.  may be null.
     */
    public String getWillDestination() {
        return options.getWillDestination();
    }

    /**
     * Get a Last Will and Testament message's payload.
     * @return the value. may be null.
     */
    public byte[] getWillPayload() {
        MqttMessage msg = options.getWillMessage();
        return msg==null ? null : msg.getPayload();
    }

    /**
     * Get a Last Will and Testament message's QOS.
     * @return the value.
     */
    public int getWillQOS() {
        MqttMessage msg = options.getWillMessage();
        return msg==null ? 0 : msg.getQos();
    }

    /**
     * Get a Last Will and Testament message's "retained" setting.
     * @return the value.
     */
    public boolean getWillRetained() {
        MqttMessage msg = options.getWillMessage();
        return msg==null ? false : msg.isRetained();
    }

    /**
     * Get the clean session setting.
     * @return the value
     */
    public boolean isCleanSession() {
        return options.isCleanSession();
    }
    
    /**
     * Get the the password to use for authentication with the server.
     * @return the value
     */
    public char[] getPassword() {
        return options.getPassword();
    }

    /**
     * Get the username to use for authentication with the server.
     * @return the value
     */
    public String getUserName() {
        return options.getUserName();
    }

    /**
     * Connection Client Id.
     * <p>
     * Optional. default null: a clientId is auto-generated.
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    /**
     * Maximum time to wait for an action (e.g., publish message) to complete.
     * <p>
     * Optional. default: -1 no timeout. 0 also means no timeout.
     * @param actionTimeToWaitMillis the time to wait in milliseconds
     */
    public void setActionTimeToWaitMillis(long actionTimeToWaitMillis) {
        this.actionTimeToWaitMillis = actionTimeToWaitMillis;
    }
    
    /**
     * QoS 1 and 2 in-flight message persistence.
     * <p>
     * optional. default: use memory persistence.
     * @param persistence the persistence implementation
     */
    public void setPersistence(MqttClientPersistence persistence) {
        this.persistence = persistence;
    }

    /**
     * Clean Session.
     * <p>
     * Qptional. default: true.
     * @param cleanSession the clean session value
     */
    public void setCleanSession(boolean cleanSession) {
        options.setCleanSession(cleanSession);
    }

    /**
     * Connection timeout.
     * Optional. 0 disables the timeout / blocks until connected. default: 30 seconds.
     * @param connectionTimeoutSec the timeout in seconds
     */
    public void setConnectionTimeout(int connectionTimeoutSec) {
        options.setConnectionTimeout(connectionTimeoutSec);
    }

    /**
     * Idle connection timeout.
     * Optional. 0 disables idle connection disconnect. default: 0 seconds (disabled).
     * <p>
     * Following an idle disconnect, the connector will automatically
     * reconnect when it receives a new tuple to publish.
     * If the connector is subscribing to topics, it will also reconnect
     * as per {@link #setSubscriberIdleReconnectInterval(int)}.
     * <p>
     * @param idleTimeoutSec the timeout in seconds
     * @see #setSubscriberIdleReconnectInterval(int)
     */
    public void setIdleTimeout(int idleTimeoutSec) {
        if (idleTimeoutSec < 0)
            idleTimeoutSec = 0;
        this.idleTimeout = idleTimeoutSec;
    }
    
    /**
     * Subscriber idle reconnect interval.
     * <p>
     * Following an idle disconnect, if the connector is subscribing to topics,
     * it will reconnect after the specified interval.
     * Optional. default: 60 seconds.
     * @param seconds the interval in seconds
     */
    public void setSubscriberIdleReconnectInterval(int seconds) {
        if (seconds < 0)
            seconds = 0;
        subscriberIdleReconnectIntervalSec = seconds;
    }

    /**
     * Connection Keep alive.
     * <p>
     * Optional. 0 disables keepalive processing. default: 60 seconds.
     * @param keepAliveSec the interval in seconds
     */
    public void setKeepAliveInterval(int keepAliveSec) {
        options.setKeepAliveInterval(keepAliveSec);
    }

    /**
     * MQTT Server URLs
     * <p>
     * Required. Must be an array of one or more MQTT server URLs.
     * When connecting, the first URL that successfully connects is used.
     * @param serverUrls the URLs
     */
    public void setServerURLs(String[] serverUrls) {
        options.setServerURIs(serverUrls);
    }

    /**
     * Last Will and Testament.
     * <p>
     * optional. default: no last-will-and-testament.
     * @param topic topic to publish to
     * @param payload the last-will-and-testament message value
     * @param qos the quality of service to use to publish the message
     * @param retained true to retain the message across connections
     */
    public void setWill(String topic, byte[] payload, int qos, boolean retained) {
        options.setWill(topic, payload, qos, retained);
    }

    /**
     * Set the password to use for authentication with the server.
     * Optional. default: null.
     * @param password the password
     */
    public void setPassword(char[] password) {
        options.setPassword(password);
    }

    /**
     * Set the username to use for authentication with the server.
     * Optional. default: null.
     * @param userName the user name
     */
    public void setUserName(String userName) {
        options.setUserName(userName);
    }

    /**
     * @param name option name
     * @param value option value. null to unset.
     */
    private void setSslOption(String name, String value) {
        Properties props = options.getSSLProperties();
        if (props == null)
            props = new Properties();
        if (value == null)
            props.remove(name);
        else
            props.setProperty(name, value);
        options.setSSLProperties(props);
    }

    /**
     * @param name option name
     * @return option's value. null if not set.
     */
    private String getSslOption(String name) {
        Properties props = options.getSSLProperties(); 
        return props == null ? null : props.getProperty(name);
    }

    /**
     * Set the SSL trust store path.
     * <p>
     * Only used with "ssl:" serverURL.
     * Path to trust store file in JKS format.
     * If not set, the standard JRE and javax.net.ssl system properties
     * control the SSL behavior.
     * Generally not required if server has a CA-signed certificate.
     * @param path the path. null to unset.
     */
    public void setTrustStore(String path) {
        setSslOption("com.ibm.ssl.trustStore", path);
    }

    /**
     * Get the SSL trust store path.
     * @return the path. null if not set.
     */
    public String getTrustStore() {
        return getSslOption("com.ibm.ssl.trustStore");
    }
    
    /**
     * Set the SSL trust store password.
     * <p>
     * Required if the trust store path is set.
     * 
     * @param password the password
     */
    public void setTrustStorePassword(char[] password) {
        setSslOption("com.ibm.ssl.trustStorePassword", new String(password));
    }

    /**
     * Get the SSL trust store path password.
     * @return the password. null if not set.
     */
    public char[] getTrustStorePassword() {
        String s = getSslOption("com.ibm.ssl.trustStorePassword");
        return s == null ? null : s.toCharArray();
    }
    
    /**
     * Set the SSL key store path.
     * <p>
     * Only used with "ssl:" serverURL when the server is configured for
     * client auth.
     * Path to trust store file in JKS format.
     * If not set, the standard JRE and javax.net.ssl system properties
     * control the SSL behavior.
     * @param path the path. null to unset.
     */
    public void setKeyStore(String path) {
        setSslOption("com.ibm.ssl.keyStore", path);
    }

    /**
     * Get the SSL trust store path.
     * @return the path. null if not set.
     */
    public String getKeyStore() {
        return getSslOption("com.ibm.ssl.keyStore");
    }
    
    /**
     * Set the SSL key store password.
     * <p>
     * Required if the key store path is set.
     * 
     * @param password the password. null to unset.
     */
    public void setKeyStorePassword(char[] password) {
        setSslOption("com.ibm.ssl.keyStorePassword", new String(password));
    }

    /**
     * Get the SSL key store path password.
     * @return the password. null if not set.
     */
    public char[] getKeyStorePassword() {
        String s = getSslOption("com.ibm.ssl.keyStorePassword");
        return s == null ? null : s.toCharArray();
    }

// paho MqttConnectOptions.setSslProperties() doesn't support this control
//    /**
//     * Set the SSL key password.
//     * <p>
//     * Defaults to using the key store password if not set.
//     * 
//     * @param password the password. null to unset.
//     */
//    public void setKeyPassword(char[] password) {
//        setSslOption("com.ibm.ssl.XXXX-NO-SUCH-KEY-XXXX.keyPassword", new String(password));
//    }
//    
//    public char[] getKeyPassword() {
//        String s = getSslOption("com.ibm.ssl.XXXX-NO-SUCH-KEY-XXXX.keyPassword");
//        return s == null ? null : s.toCharArray();
//    }

 // paho MqttConnectOptions.setSslProperties() doesn't support this control
//    /**
//     * Set the SSL key certificate alias.
//     * <p>
//     * Defaults to using "default" if not set.
//     * @param alias the alias. null to unset.
//     */
//    public void setKeyCertificateAlias(String alias) {
//        setSslOption("com.ibm.ssl.XXXX-NO-SUCH-KEY-XXXX.keyCertificateAlias", alias);
//    }
//
//    public String getKeyCertificateAlias() {
//        return getSslOption("com.ibm.ssl.XXXX-NO-SUCH-KEY-XXXX.keyCertificateAlias");
//    }

    /**
     * INTERNAL USE ONLY.
     * @return object
     */
    public Object options() {
        return options;
    }
}
