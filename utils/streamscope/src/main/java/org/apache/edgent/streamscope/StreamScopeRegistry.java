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
package org.apache.edgent.streamscope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A registry for Stream "oscilloscope" {@link StreamScope} instances.
 * <P>
 * The registry contains a collection of StreamScope instances
 * that are registered by one or more names.
 * </P><P>
 * The names are: by a TStream {@link org.apache.edgent.topology.TStream#alias(String) alias} or
 * by a stream's (output port's) unique identifier.
 * Static methods are provided for composing these names and extracting
 * the alias/identifier from generated names.
 * </P>
 * See {@code org.apache.edgent.providers.development.DevelopmentProvider}
 */
public class StreamScopeRegistry {
  private final Map<String, StreamScope<?>> byNameMap = new HashMap<>();
  private final Map<StreamScope<?>, List<String>> byStreamScopeMap = new HashMap<>();
  private static final String JOB_OPLET_FMT = "j[%s].op[%s]";
  private static final String STREAMID_FMT = JOB_OPLET_FMT+".o[%d]";
  private static final String ID_PREFIX = "id.";
  private static final String ALIAS_PREFIX = "alias.";

  public StreamScopeRegistry() {
    
  }

  /**
   * Make a streamId for the specified stream.
   * @param jobId the job id (e.g., "JOB_0")
   * @param opletId the oplet id (e.g., "OP_2")
   * @param oport the oplet output port index (0-based)
   * @return the streamId
   */
  public static String mkStreamId(String jobId, String opletId, int oport) {
    Objects.requireNonNull(jobId, "jobId");
    Objects.requireNonNull(opletId, "opletId");
    if (oport < 0)
      throw new IllegalArgumentException("oport");
    return String.format(STREAMID_FMT, jobId, opletId, oport);
  }

  /** create a prefix of a streamId based name
   * @param jobId the job id (e.g., "JOB_0")
   * @param opletId the oplet id (e.g., "OP_2")
   * @return the value
   */
  static String mkStreamIdNamePrefix(String jobId, String opletId) {
    return String.format(ID_PREFIX+JOB_OPLET_FMT, jobId, opletId);
  }
  
  /** create a registration name for a stream alias
   * @param alias the alias
   * @return the value
   */
  public static String nameForStreamAlias(String alias) {
    Objects.requireNonNull(alias, "alias");
    return ALIAS_PREFIX+alias;
  }
  
  /** Create a registration name for a stream id.
   * @param streamId the stream id
   * @return the value
   * @see #mkStreamId(String, String, int)
   */
  public static String nameForStreamId(String streamId) {
    Objects.requireNonNull(streamId, "id");
    return ID_PREFIX+streamId;
  }
  
  /** Extract the stream alias from a name
   * @param name the name 
   * @return null if {@code name} is not from nameByStreamAlias()
   */
  public static String streamAliasFromName(String name) {
    Objects.requireNonNull(name, "name");
    if (!name.startsWith(ALIAS_PREFIX))
      return null;
    return name.substring(ALIAS_PREFIX.length());
  }
  
  /** Extract the streamId from the name.
   * @param name the name 
   * @return null if {@code name} is not from nameByStreamId()
   */
  public static String streamIdFromName(String name) {
    Objects.requireNonNull(name, "name");
    if (!name.startsWith(ID_PREFIX))
      return null;
    return name.substring(ID_PREFIX.length());
  }
  
  /** Register a StreamScope by {@code name}
   * <P>
   * A single StreamScope can be registered with multiple names.
   * </P>
   * @param name name to register with
   * @param streamScope the StreamScope
   * @throws IllegalStateException if a registration already exists for {@code name}
   * @see #nameForStreamId(String)
   * @see #nameForStreamAlias(String)
   */
  public synchronized void register(String name, StreamScope<?> streamScope) {
    if (byNameMap.containsKey(name))
      throw new IllegalStateException("StreamScope already registered by name "+name);
    byNameMap.put(name, streamScope);
    List<String> names = byStreamScopeMap.get(streamScope); 
    if (names == null) {
      names = new ArrayList<>(2);
      byStreamScopeMap.put(streamScope, names);
    }
    names.add(name);
  }
  
  /**
   * Lookup a StreamScope
   * @param name a StreamScope is registration name
   * @return the StreamScope. null if name is not registered.
   * @see #nameForStreamId(String)
   * @see #nameForStreamAlias(String)
   */
  public synchronized StreamScope<?> lookup(String name) {
    return byNameMap.get(name);
  }
  
  /**
   * Get the registered names.
   * @return unmodifiable collection of the name.
   * The set is backed by the registry so the set may change.
   */
  public synchronized Set<String> getNames() {
    return Collections.unmodifiableSet(byNameMap.keySet());
  }
  
  /** Get registered StreamScopes and the name(s) each is registered with.
   * The map is backed by the registry so its contents may change.
   * @return the map
   */
  public synchronized Map<StreamScope<?>, List<String>> getStreamScopes() {
    return Collections.unmodifiableMap(byStreamScopeMap);
  }
  
  /** remove the specific name registration.  Other registration of the same StreamScope may still exist.
   * no-op if name is not registered.
   * @param name the name to unregister
   * @see #unregister(StreamScope)
   */
  public synchronized void unregister(String name) {
    StreamScope<?> streamScope = byNameMap.remove(name);
    if (streamScope == null)
      return;
    List<String> names = byStreamScopeMap.get(streamScope);
    names.remove(name);
    if (names.isEmpty())
      byStreamScopeMap.remove(streamScope);
  }
  
  /** remove all name registrations of the StreamScope.
   * no-op if no registrations for the StreamScope
   * @param streamScope the StreamScope to unregister
   */
  public synchronized void unregister(StreamScope<?> streamScope) {
    List<String> names = byStreamScopeMap.get(streamScope);
    if (names == null)
      return;
    names = new ArrayList<>(names);
    for (String name : names)
      unregister(name);
  }
  
  /** remove all name registrations of the StreamScopes for the specified oplet.
   * no-op if no registrations for the oplet
   * @param jobId the job id (e.g., "JOB_0")
   * @param opletId the oplet id (e.g., "OP_2")
   */
  synchronized void unregister(String jobId, String opletId) {
    String prefix = mkStreamIdNamePrefix(jobId, opletId);
    List<StreamScope<?>> toUnregister = new ArrayList<>();
    for (String name : getNames()) {
      if (name.startsWith(prefix)) {
        toUnregister.add(lookup(name));
      }
    }
    for (StreamScope<?> streamScope : toUnregister) {
      unregister(streamScope);
    }
  }
  
}

