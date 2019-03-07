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

package org.apache.edgent.connectors.iotp.runtime;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.iotf.client.gateway.Command;

/**
 * An event setup adapter for {@link Topology#events(Consumer) org.apache.edgent.org.apache.edgent.topology.events()}
 * that submits received WIoTP device commands as stream tuples.
 */
public class IotpGWCommands implements Consumer<Consumer<Command>> {
    private static final long serialVersionUID = 1L;
    private final IotpGWConnector connector;
    private static final Logger logger = LoggerFactory.getLogger(IotpGWCommands.class);

    public IotpGWCommands(IotpGWConnector connector) {
        this.connector = connector;
    }

    @Override
    public void accept(Consumer<Command> commandSubmitter) {
        
        try {
            connector.subscribeCommands(commandSubmitter);
        } catch (Exception e) {
            logger.error("Exception caught while subscribing commands", e);
        }
    }
}
