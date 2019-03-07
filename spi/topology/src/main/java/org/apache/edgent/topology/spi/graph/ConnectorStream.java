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
package org.apache.edgent.topology.spi.graph;

import static org.apache.edgent.function.Functions.synchronizedFunction;
import static org.apache.edgent.window.Policies.alwaysInsert;
import static org.apache.edgent.window.Policies.evictOlderWithProcess;
import static org.apache.edgent.window.Policies.insertionTimeList;
import static org.apache.edgent.window.Policies.scheduleEvictIfEmpty;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Functions;
import org.apache.edgent.function.Predicate;
import org.apache.edgent.function.ToIntFunction;
import org.apache.edgent.graph.Connector;
import org.apache.edgent.graph.Graph;
import org.apache.edgent.graph.Vertex;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.oplet.core.FanIn;
import org.apache.edgent.oplet.core.Pipe;
import org.apache.edgent.oplet.core.Sink;
import org.apache.edgent.oplet.core.Split;
import org.apache.edgent.oplet.core.Union;
import org.apache.edgent.oplet.functional.Filter;
import org.apache.edgent.oplet.functional.FlatMap;
import org.apache.edgent.oplet.functional.Map;
import org.apache.edgent.oplet.functional.Peek;
import org.apache.edgent.oplet.window.Aggregate;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.spi.AbstractTStream;
import org.apache.edgent.window.Partition;
import org.apache.edgent.window.Policies;
import org.apache.edgent.window.Window;
import org.apache.edgent.window.Windows;

/**
 * A stream that directly adds oplets to the org.apache.edgent.graph.
 *
 * @param <G> org.apache.edgent.org.apache.edgent.topology type
 * @param <T> tuple type
 */
public class ConnectorStream<G extends Topology, T> extends AbstractTStream<G, T> {

    private final Connector<T> connector;

    protected ConnectorStream(G topology, Connector<T> connector) {
        super(topology);
        this.connector = connector;
    }

    protected <U> ConnectorStream<G, U> derived(Connector<U> connector) {
        return new ConnectorStream<G, U>(topology(), connector);
    }

    protected Graph graph() {
        return connector.graph();
    }

    protected <N extends Pipe<T, U>, U> TStream<U> connectPipe(N pipeOp) {
        return derived(graph().pipe(connector, pipeOp));
    }

    @Override
    public TStream<T> filter(Predicate<T> predicate) {
        return connectPipe(new Filter<T>(predicate));
    }

    @Override
    public <U> TStream<U> map(Function<T, U> mapper) {
        mapper = synchronizedFunction(mapper);
        return connectPipe(new Map<T, U>(mapper));
    }

    @Override
    public <U> TStream<U> flatMap(Function<T, Iterable<U>> mapper) {
        return connectPipe(new FlatMap<T, U>(mapper));
    }

    @Override
    public List<TStream<T>> split(int n, ToIntFunction<T> splitter) {
        if (n <= 0)
            throw new IllegalArgumentException("n <= 0");

        Split<T> splitOp = new Split<T>(splitter);

        Vertex<Split<T>, T, T> splitVertex = graph().insert(splitOp, 1, n);
        connector.connect(splitVertex, 0);

        List<TStream<T>> outputs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            outputs.add(derived(splitVertex.getConnectors().get(i)));
        }

        return outputs;
    }

    @Override
    public <E extends Enum<E>> EnumMap<E,TStream<T>> split(Class<E> enumClass, Function<T, E> splitter) {

        E[] es = enumClass.getEnumConstants();

        List<TStream<T>> outputs = split(es.length, t -> {
            E split = splitter.apply(t);
            return split != null ? split.ordinal() : -1;
        });

        EnumMap<E,TStream<T>> returnMap = new EnumMap<>(enumClass);

        for (E e : es) {
            returnMap.put(e, outputs.get(e.ordinal()));
        }

        return returnMap;
    }

    @Override
    public TStream<T> peek(Consumer<T> peeker) {
        peeker = Functions.synchronizedConsumer(peeker);
        connector.peek(new Peek<T>(peeker));
        return this;
    }
    
    @Override
    public TSink<T> sink(Sink<T> oplet) {
        Vertex<Sink<T>, T, Void> sinkVertex = graph().insert(oplet, 1, 0);
        connector.connect(sinkVertex, 0);
        return new ConnectorSink<>(this);
    }

    @Override
    public <U> TStream<U> pipe(Pipe<T, U> pipe) {
        return connectPipe(pipe);
    }

    @Override
    public <U> TStream<U> fanin(FanIn<T,U> fanin, List<TStream<T>> others) {
      if (others.isEmpty() || others.size() == 1 && others.contains(this)) 
        throw new IllegalArgumentException("others");  // use pipe()
      if (new HashSet<>(others).size() != others.size())
        throw new IllegalArgumentException("others has dups");
      
      for (TStream<T> other : others)
          verify(other);
      
      others = new ArrayList<>(others);
      others.add(0, this);
      
      Vertex<Oplet<T,U>, T, U> fanInVertex = graph().insert(fanin, others.size(), 1);
      int inputPort = 0;
      for (TStream<T> other : others) {
          @SuppressWarnings("unchecked")
          ConnectorStream<G,T> cs = (ConnectorStream<G, T>) other;
          cs.connector.connect(fanInVertex, inputPort++);
      }
          
      return derived(fanInVertex.getConnectors().get(0));
    }

    @Override
    public <K> TWindow<T, K> last(int count, Function<T, K> keyFunction) {
        TWindowImpl<T, K> window = new TWindowImpl<T, K>(count, this, keyFunction);
        return window;
    }
    

    @Override
    public <K> TWindow<T, K> last(long time, TimeUnit unit,
            Function<T, K> keyFunction) {
        TWindowTimeImpl<T, K> window = new TWindowTimeImpl<T, K>(time, unit, this, keyFunction);
        return window;
    }
    
    @Override
    public <J, U, K> TStream<J> join(Function<T, K> keyer,
            TWindow<U, K> twindow, BiFunction<T, List<U>, J> joiner) {
        
        TStream<U> lastStream = twindow.feeder();
        BiFunction<List<U>,K, Object> processor = Functions.synchronizedBiFunction((list, key) -> null);
        Window<U, K, ?> window;
        if(twindow instanceof TWindowImpl){   
            window = Windows.lastNProcessOnInsert(((TWindowImpl<U, K>)twindow).getSize(), twindow.getKeyFunction());
            
        }
        
        else if (twindow instanceof TWindowTimeImpl){
            long time = ((TWindowTimeImpl<U, K>)(twindow)).getTime();
            TimeUnit unit = ((TWindowTimeImpl<U, K>)(twindow)).getUnit();
            window = Windows.window(
                            alwaysInsert(),
                            scheduleEvictIfEmpty(time, unit),
                            evictOlderWithProcess(time, unit),
                            Policies.doNothing(),
                            twindow.getKeyFunction(),
                            insertionTimeList());
        }
        else{
            throw new IllegalStateException("Unsupported window format");
        }
        
        // To perform a join, the runtime needs to maintain a windowImpl based on
        // the tuples from the twindow.feeder TStream. To do this, it's 
        // necessary to create an Aggregate oplet and insert it into the
        // org.apache.edgent.graph with lastStream.pipe.
        Aggregate<U,Object,K> op = new Aggregate<U,Object,K>(window, processor);
        lastStream.pipe(op);
        
        return this.map((tuple) -> {
            // The window object can be referenced via closure, and the corresponding
            // partition can be retrieved based on the keyer. This way, we avoid
            // needing to create an additional oplet type with multiple input ports.
           
            java.util.Map<K, ?> partitions = window.getPartitions();
            Partition<U, K, ? extends List<U>> part;
            synchronized(partitions){
                part = window.getPartitions().get(keyer.apply(tuple));
            }
            if(part == null)
                return null;
            J ret;
            synchronized (part) {
                List<U> last = part.getContents();
                ret = joiner.apply(tuple, last);
            }
            return ret;
        });
    }

    @Override
    public <J, U, K> TStream<J> joinLast(Function<T, K> keyer,
            TStream<U> lastStream, Function<U, K> lastStreamKeyer, BiFunction<T, U, J> joiner) {
        BiFunction<List<U>,K, Object> processor = Functions.synchronizedBiFunction((list, key) -> null);
        Window<U, K, LinkedList<U>> window = Windows.lastNProcessOnInsert(1, lastStreamKeyer);
        Aggregate<U,Object,K> op = new Aggregate<U,Object,K>(window, processor);
        lastStream.pipe(op);
        return this.map((tuple) -> {
            Partition<U, K, ? extends List<U>> part = window.getPartitions().get(keyer.apply(tuple));
            if(part == null)
                return null;
            J ret;
            synchronized (part) {
                U last = part.getContents().get(0);
                ret = joiner.apply(tuple, last);
            }
            return ret;
        });
    }
    
    @Override
    public TStream<T> union(Set<TStream<T>> others) {
        if (others.isEmpty())
            return this;
        if (others.size() == 1 && others.contains(this))
            return this;
        
        for (TStream<T> other : others)
            verify(other);
        
        // Create a set we can modify and add this stream
        others = new HashSet<>(others);
        others.add(this);
        
        Union<T> fanInOp = new Union<T>();

        Vertex<Union<T>, T, T> fanInVertex = graph().insert(fanInOp, others.size(), 1);
        int inputPort = 0;
        for (TStream<T> other : others) {
            @SuppressWarnings("unchecked")
            ConnectorStream<G,T> cs = (ConnectorStream<G, T>) other;
            cs.connector.connect(fanInVertex, inputPort++);
        }
            
        return derived(fanInVertex.getConnectors().get(0));
    }

    @Override
    public TStream<T> tag(String... values) {
        connector.tag(values);
        return this;
    }

    @Override
    public Set<String> getTags() {
        return connector.getTags();
    }

    @Override
    public TStream<T> alias(String alias) {
        connector.alias(alias);
        return this;
    }

    @Override
    public String getAlias() {
        return connector.getAlias();
    }
    
    /**
     * Intended only as a debug aid and content is not guaranteed. 
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + " alias=" + getAlias()
                + " tags=" + getTags();
    }

}
