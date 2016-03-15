/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015, 2016 
*/
package quarks.topology.spi.graph;

import quarks.function.*;
import quarks.graph.Connector;
import quarks.graph.Graph;
import quarks.graph.Vertex;
import quarks.oplet.core.Pipe;
import quarks.oplet.core.Sink;
import quarks.oplet.core.Split;
import quarks.oplet.core.Union;
import quarks.oplet.functional.Filter;
import quarks.oplet.functional.FlatMap;
import quarks.oplet.functional.Map;
import quarks.oplet.functional.Peek;
import quarks.topology.TSink;
import quarks.topology.TStream;
import quarks.topology.TWindow;
import quarks.topology.Topology;
import quarks.topology.spi.AbstractTStream;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static quarks.function.Functions.synchronizedFunction;

/**
 * A stream that directly adds oplets to the graph.
 *
 * @param <G>
 * @param <T>
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

        List<TStream<T>> outputs = split(es.length, e -> IntStream.range(0, es.length).filter(i-> es[i].equals(splitter.apply(e)))
            .mapToObj(i -> es[i].ordinal()).findAny().orElse(-1));

        EnumMap<E,TStream<T>> returnMap = new EnumMap<>(enumClass);
        for (int i = 0; i < es.length ; i++){
            returnMap.put(es[i], outputs.get(es[i].ordinal()));
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

}
