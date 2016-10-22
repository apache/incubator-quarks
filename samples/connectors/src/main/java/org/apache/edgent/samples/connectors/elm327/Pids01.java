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
package org.apache.edgent.samples.connectors.elm327;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.edgent.samples.connectors.elm327.runtime.CommandExecutor;

import com.google.gson.JsonObject;

/**
 * OBD-II Standard Mode 01 Pids.
 *
 * 
 * @see <a href="https://en.wikipedia.org/wiki/OBD-II_PIDs#Mode_01">OBD-II Mode 01 Pids</a>
 */
public enum Pids01 implements Cmd {
    
    /**
     * Get the list of available PIDs.
     */
	AVAILABLE_PIDS("00"),
	
	/**
	 * Engine coolant temperature in degrees C.
	 */
	ENGINE_COOLANT_TEMP("05") {
		@Override
		protected boolean decode(JsonObject result, byte[] reply) {
			
			int[] binary = CommandExecutor.binary(reply, 4, 2);
			
			int c = binary[0] - 40;
			result.addProperty(VALUE, c);
			
			return true;
		}
	},

	/**
	 * Engine speed in rpm.
	 */
	RPM("0C") {
		@Override
		protected boolean decode(JsonObject result, byte[] reply) {
			
			int[] binary = CommandExecutor.binary(reply, 4, 4);
			int rpm = ((binary[0] * 256) + binary[1])/4;
			result.addProperty(VALUE, rpm);
			
			return true;
		}
	},
	
	/**
	 * Vehicle speed in km/h.
	 */
	SPEED("0D"){
		@Override
		protected boolean decode(JsonObject result, byte[] reply) {
			
			int[] binary = CommandExecutor.binary(reply, 4, 2);
			
			result.addProperty(VALUE, binary[0]);
			
			return true;
		}
	},
	
	/**
     * Engine air intake temperature in degrees C.
     */
	AIR_INTAKE_TEMP("0F"){
		@Override
		protected boolean decode(JsonObject result, byte[] reply) {
			
			int[] binary = CommandExecutor.binary(reply, 4, 2);
			
			int c = binary[0] - 40;
			result.addProperty(VALUE, c);
			
			return true;
		}
	},
	;

    private final String pid;
	private final byte[] cmd;
	
	Pids01(String pid) {
		this.pid = pid;
		cmd = ("01" + pid + "1\r").getBytes(StandardCharsets.US_ASCII);
	}
	
	public String id() {
		return pid;
	}
	
	@Override
	public void writeCmd(OutputStream out) throws IOException {
		out.write(cmd);
	}
	@Override
	public final boolean result(JsonObject result, byte[] data) {
		return validateReply(data) && decode(result, data);
	}
	 boolean decode(JsonObject result, byte[] data) {
		 return true;
	 }
	
	boolean validateReply(byte[] reply) {
		if (reply[0] != '4')
			return false;
		if (reply[1] != '1')
			return false;
		if (reply[2] != pid.charAt(0))
			return false;
		if (reply[3] != pid.charAt(1))
			return false;
		
		return true;
	}
}
