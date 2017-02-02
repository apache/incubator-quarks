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
package org.apache.edgent.providers.iot;

import static org.apache.edgent.topology.services.ApplicationService.SYSTEM_APP_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.edgent.apps.iot.IotDevicePubSub;
import org.apache.edgent.apps.runtime.JobMonitorApp;
import org.apache.edgent.connectors.iot.Commands;
import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.pubsub.service.ProviderPubSub;
import org.apache.edgent.connectors.pubsub.service.PublishSubscribeService;
import org.apache.edgent.execution.DirectSubmitter;
import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.runtime.appservice.AppService;
import org.apache.edgent.runtime.jsoncontrol.JsonControlService;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.TopologyProvider;
import org.apache.edgent.topology.mbeans.ApplicationServiceMXBean;
import org.apache.edgent.topology.services.ApplicationService;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * IoT provider supporting multiple topologies with a single connection to a
 * message hub. A provider that uses a single {@link IotDevice} to communicate
 * with an IoT scale message hub.
 * {@link org.apache.edgent.connectors.pubsub.PublishSubscribe Publish-subscribe} is
 * used to allow multiple topologies to communicate through the single
 * connection.
 * <P>
 * This provider registers these services:
 * <UL>
 * <LI>{@link ControlService control} - An instance of {@link JsonControlService}.</LI>
 * <LI>{@link ApplicationService application} - An instance of {@link AppService}.</LI>
 * <LI>{@link PublishSubscribeService publish-subscribe} - An instance of {@link ProviderPubSub}</LI>
 * <LI>preferences (optional) - An instance of {@code java.util.pref.Preferences} to store application
 * and service preferences. A {@code Preferences} node is created if the provider is created with
 * a name that is not {@code null}. If the preferences implementation supports persistence
 * then any preferences will be maintained across provider and JVM restarts when creating a
 * provider with the same name. The {@code Preferences} node is a user node.
 * </UL>
 * System applications provide this functionality:
 * <UL>
 * <LI>Single connection to the message hub using an {@code IotDevice}
 * using {@link IotDevicePubSub}.
 * Applications using this provider that want to connect
 * to the message hub for device events and commands must create an instance of
 * {@code IotDevice} using {@link IotDevicePubSub#addIotDevice(org.apache.edgent.topology.TopologyElement)}</LI>
 * <LI>Access to the control service through device commands from the message hub using command
 * identifier {@link Commands#CONTROL_SERVICE edgentControl}.
 * </UL>
 * <P>
 * An {@code IotProvider} is created with a provider and submitter that it delegates
 * the creation and submission of topologies to.
 * </P>
 * 
 * @see IotDevice
 * @see IotDevicePubSub
 */
public class IotProvider implements TopologyProvider,
 DirectSubmitter<Topology, Job> {
    
    /**
     * IoT control using device commands application name.
     */
    public static final String CONTROL_APP_NAME = SYSTEM_APP_PREFIX + "IotCommandsToControl";
    
    private final String name;
    private final TopologyProvider provider;
    private final Function<Topology, IotDevice> iotDeviceCreator;
    private final DirectSubmitter<Topology, Job> submitter;
    
    /**
     * System applications by name.
     */
    private final List<String> systemApps = new ArrayList<>();

    private final Map<String,JsonObject> autoSubmitApps = new HashMap<>();  // <appName,config>

    private JsonControlService controlService = new JsonControlService();
    
    /**
     * Create an {@code IotProvider} that uses its own {@code DirectProvider}.
     * No name is assigned to the provider so a preferences service is not created.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     * @see DirectProvider
     */
    public IotProvider(Function<Topology, IotDevice> iotDeviceCreator) {   
        this(null, new DirectProvider(), iotDeviceCreator);
    }
    
    /**
     * Create an {@code IotProvider} that uses its own {@code DirectProvider}.
     * @param name Name of the provider, if the value is not {@code null} then a preferences service is created.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     * @see DirectProvider
     */
    public IotProvider(String name, Function<Topology, IotDevice> iotDeviceCreator) {   
        this(name, new DirectProvider(), iotDeviceCreator);
    }
    
    /**
     * Create an {@code IotProvider} that uses the passed in {@code DirectProvider}.
     * 
     * @param name Name of the provider, if the value is not {@code null} then a preferences service is created.
     * @param provider {@code DirectProvider} to use for topology creation and submission.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     * @see DirectProvider
     *
     */
    public IotProvider(String name, DirectProvider provider, Function<Topology, IotDevice> iotDeviceCreator) {
        this(name, provider, provider, iotDeviceCreator);
    }

    /**
     * Create an {@code IotProvider}.
     * @param name Name of the provider, if the value is not {@code null} then a preferences service is created.
     * @param provider How topologies are created.
     * @param submitter How topologies will be submitted.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     */
    public IotProvider(String name, TopologyProvider provider, DirectSubmitter<Topology, Job> submitter,
            Function<Topology, IotDevice> iotDeviceCreator) {
        this.name = name;
        this.provider = provider;
        this.submitter = submitter;
        this.iotDeviceCreator = iotDeviceCreator;
        
        registerControlService();
        registerApplicationService();
        registerPublishSubscribeService();       
        registerPreferencesService();
        
        createIotDeviceApp();
        createIotCommandToControlApp();
        createJobMonitorApp();
    }
    
    /**
     * Return the name of this provider.
     * @return Provider's name, can be {@code null}.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the application service.
     * Callers may use this to register applications to
     * be executed by this provider.
     * @return application service.
     */
    public ApplicationService getApplicationService() {
        return getServices().getService(ApplicationService.class);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceContainer getServices() {
        return submitter.getServices();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final Topology newTopology() {
        return provider.newTopology();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final Topology newTopology(String name) {
        return provider.newTopology(name);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final Future<Job> submit(Topology topology) {
        return submitter.submit(topology);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final Future<Job> submit(Topology topology, JsonObject config) {
        return submitter.submit(topology, config);
    }

    protected void registerControlService() {
        getServices().addService(ControlService.class, getControlService());
    }

    protected void registerApplicationService() {
        AppService.createAndRegister(this, this);
    }
    protected void registerPublishSubscribeService() {
        getServices().addService(PublishSubscribeService.class, 
                new ProviderPubSub());
    }
    
    protected void registerPreferencesService() {
        if (getName() == null)
            return;
        Preferences providerNode = getPreferences(getName());
        getServices().addService(Preferences.class, providerNode);
        
        try {
            providerNode.flush();
        } catch (BackingStoreException e) {
            // TODO log that preference changes may not be persisted
            ;
        }
    }
    
    /**
     * Get the Preferences node that will be used for the IotProvider with the specified name.
     * @param providerName The value that will be passed into {@link IotProvider#IotProvider(String, TopologyProvider, DirectSubmitter, Function) IotProvider()}
     * @return Preferences
     */
    public static Preferences getPreferences(String providerName) {
      Preferences classNode = Preferences.userNodeForPackage(IotProvider.class);
      Preferences providerNode = classNode.node(providerName);
      return providerNode;
    }

    protected JsonControlService getControlService() {
        return controlService;
    }
    
    /**
     * Create application that connects to the message hub.
     * Subscribes to device events and sends them to the messages hub.
     * Publishes device commands from the message hub.
     * @see IotDevicePubSub
     * @see #createMessageHubDevice(Topology)
     */
    protected void createIotDeviceApp() {
        
        getApplicationService().registerTopology(IotDevicePubSub.APP_NAME,
                (topology, config) -> IotDevicePubSub.createApplication(createMessageHubDevice(topology)));

        systemApps.add(IotDevicePubSub.APP_NAME);
    }
    
    /**
     * Create Job monitor application.
     * @see JobMonitorApp
     */
    protected void createJobMonitorApp() {
        
        getApplicationService().registerTopology(JobMonitorApp.APP_NAME,
                (topology, config) -> JobMonitorApp.declareTopology(topology));

        systemApps.add(JobMonitorApp.APP_NAME);
    }
    
    /**
     * Create application connects {@code edgentControl} device commands
     * to the control service.
     * 
     * Subscribes to device
     * commands of type {@link Commands#CONTROL_SERVICE}
     * and sends the payload into the JSON control service
     * to invoke the control operation.
     */
    protected void createIotCommandToControlApp() {
         
        this.registerTopology(CONTROL_APP_NAME, (iotDevice, config) -> {
            TStream<JsonObject> controlCommands = iotDevice.commands(Commands.CONTROL_SERVICE);
            controlCommands.sink(cmd -> {                
                try {
                    getControlService().controlRequest(cmd.getAsJsonObject(IotDevice.CMD_PAYLOAD));
                } catch (Exception re) {
                    // If the command fails then don't stop this application,
                    // just process the next command.
                    LoggerFactory.getLogger(ControlService.class).error("Control request failed: {}", cmd, re);
                }
            });
        });

        systemApps.add(CONTROL_APP_NAME);
    }
    
    /**
     * Start this provider by starting its system applications 
     * and any autoSubmit-enabled registered applications.
     * 
     * @throws Exception on failure starting applications.
     */
    public void start() throws Exception {
        ApplicationServiceMXBean bean = getControlService().getControl(ApplicationServiceMXBean.TYPE,
                ApplicationService.ALIAS, ApplicationServiceMXBean.class);
        
        for (String systemAppName : systemApps) {
            bean.submit(systemAppName, null /* no config */);
        }
        
        for (Entry<String,JsonObject> e : autoSubmitApps.entrySet()) {
          submitApplication(e.getKey(), e.getValue());
        }
    }

    /**
     * Create the connection to the message hub.
     * 
     * Creates an instance of {@link IotDevice}
     * used to communicate with the message hub. This
     * provider creates and submits an application
     * that subscribes to published events to send
     * as device events and publishes device commands.
     * <BR>
     * The application is created using
     * {@link IotDevicePubSub#createApplication(IotDevice)}.
     * <BR>
     * The {@code IotDevice} is created using the function
     * passed into the constructor.
     * 
     * @param topology Topology the {@code IotDevice} will be contained in.
     * @return IotDevice device used to communicate with the message hub.
     * 
     * @see IotDevice
     * @see IotDevicePubSub
     */
    protected IotDevice createMessageHubDevice(Topology topology) {
        return iotDeviceCreator.apply(topology);
    }
    
    /**
     * Register an application that uses an {@code IotDevice}.
     * <BR>
     * Same as {@link #registerTopology(String, BiConsumer, boolean, JsonObject) registerTopology(appName, builder, false, null)}.
     * 
     * @param applicationName Application name
     * @param builder Function that builds the topology.
     */
    public void registerTopology(String applicationName, BiConsumer<IotDevice, JsonObject> builder) {
      registerTopology(applicationName, builder, false, null);
    }
    
    /**
     * Register an application that uses an {@code IotDevice}.
     * <BR>
     * Wrapper around {@link ApplicationService#registerTopology(String, BiConsumer)}
     * that passes in an {@link IotDevice} and configuration to the supplied
     * function {@code builder} that builds the application. The passed
     * in {@code IotDevice} is created using {@link IotDevicePubSub#addIotDevice(org.apache.edgent.topology.TopologyElement)}.
     * <BR>
     * Note that {@code builder} obtains a reference to its topology using
     * {@link IotDevice#topology()}.
     * <P>
     * When the application is
     * {@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean#submit(String, String) submitted} {@code builder.accept(iotDevice, config)}
     * is called to build the application's graph.
     * </P>
     * <P>
     * Specify {@code autoSubmit==true}, to have the provider submit the application
     * when {@link #start} is called.
     * </P>
     * 
     * @param applicationName Application name
     * @param builder Function that builds the topology.
     * @param autoSubmit auto submit the application when {@link #start()} is called.
     * @param config config for auto-submitted application.
     *        See {@link #submit(Topology, JsonObject) submit}. May be null.
     */
    public void registerTopology(String applicationName, BiConsumer<IotDevice, JsonObject> builder, boolean autoSubmit, JsonObject config) {
        getApplicationService().registerTopology(applicationName,
                (topology,cfg) -> builder.accept(IotDevicePubSub.addIotDevice(topology), cfg));
        if (autoSubmit) {
          autoSubmitApps.put(applicationName, config);
        }
    }

    /**
     * Submit the specified application previously registered
     * via {@link #registerTopology(String, BiConsumer) registerTopology}.
     * @param appName name of registered application
     * @param config See {@link #submit(Topology, JsonObject) submit}. May be null.
     * 
     * @throws Exception on failure starting applications.
     */
    private void submitApplication(String appName, JsonObject config) throws Exception {
      if (systemApps.contains(appName)) {
        throw new IllegalArgumentException("appName");
      }
      ApplicationServiceMXBean bean = getControlService().getControl(ApplicationServiceMXBean.TYPE,
          ApplicationService.ALIAS, ApplicationServiceMXBean.class);
      bean.submit(appName, config==null ? null : config.toString());
    }
}
