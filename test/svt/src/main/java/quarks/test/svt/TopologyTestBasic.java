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
package quarks.test.svt;

import java.io.PrintWriter;

/*
 * Basic topology test to exercise TStream methods.
 * 
 * Run from the quarks top-level directory: 
 * java -cp "test/svt/classes;target/java8/lib/*;target/java8/ext/slf4j-1.7.12/*" 
 *      quarks.test.svt.TopologyTestBasic <option>
 * 
 * Option:
 *     console - specify to enable the console for this application. 
 *               The console URL will be written into file consoleurl.txt.
 *  *            
 * To-Do:  windowing
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import quarks.console.server.HttpServer;
import quarks.metrics.Metrics;
import quarks.providers.development.DevelopmentProvider;
import quarks.topology.TStream;
import quarks.topology.Topology;

public class TopologyTestBasic {
    public static void main(String[] args) throws Exception {
        
        System.out.println("TopologyTestBasic start");
        boolean console = false;
        if (args.length == 1 && args[0].toLowerCase().equals("console"))
            console = true;
        
        DevelopmentProvider tp = new DevelopmentProvider(); 
          
        Topology t = tp.newTopology("TopologyTestBasic");
        Topology t2 = tp.newTopology("TopologyTestBasic2");
        
        Metrics.counter(t);
        Metrics.counter(t2);         

        //**************************************************************	        
        //Source 1
        //**************************************************************
        Random r1 = new Random();
        TStream<Double> gaussian = t.poll(() -> r1.nextGaussian(), 100, TimeUnit.MILLISECONDS);
        gaussian = Metrics.counter(gaussian);
        
        // testing Peek!
        gaussian = gaussian.peek(g -> System.out.println("R1:" + g));

        // A filter
        gaussian = gaussian.filter(g -> g < 10000000);
        gaussian.print();
        
        // A modify
        gaussian = gaussian.modify(g-> g*3 + 1);
        
        //Add counter 
        gaussian = Metrics.counter(gaussian);

        //A map  
        TStream<String> s1 = gaussian.map(g -> "g1: " + g.toString()).tag("s1", "gaussian");
        s1.sink(tuple -> {});

        //A split into 11 streams		 
        List<TStream<Double>> splits1 = gaussian.split(11, tuple -> {
            switch (tuple.toString().charAt(0)) {
            case '-':              //negative numbers
                return 10;
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            default:
                return 10;
            }
        });
        
        TStream<Double> sp0 = splits1.get(0).tag("split","sp0");
        sp0 = Metrics.counter(sp0);
        Metrics.rateMeter(sp0);
        sp0.print();
        
        //Add a second tag
        TStream<Double> sp0_1 = sp0.tag("split","sp0_1");
        sp0_1.print();
        
        
        TStream<Double> sp1 =splits1.get(1).tag("split","sp1");
        sp1 = Metrics.counter(sp1);
        sp1.print();
        
        TStream<Double> sp2 =splits1.get(2).tag("split","sp2");
        sp2 = Metrics.counter(sp2);
        sp2.print();
        
        TStream<Double> sp3 =splits1.get(3).tag("split","sp3");
        sp3 = Metrics.counter(sp3);
        sp3.print();
        
        TStream<Double> sp4 =splits1.get(4).tag("split","sp4");
        sp4 = Metrics.counter(sp4);
        sp4.print();
        
        TStream<Double> sp5 =splits1.get(5).tag("split","sp5");
        sp5 = Metrics.counter(sp1);
        sp5.print();
        
        TStream<Double> sp6 =splits1.get(6).tag("split","sp6");
        sp6 = Metrics.counter(sp6);
        sp6.print();
        
        TStream<Double> sp7 =splits1.get(7).tag("split","sp7");
        sp7 = Metrics.counter(sp7);
        sp7.print();
        
        TStream<Double> sp8 =splits1.get(8).tag("split","sp8");
        sp8 = Metrics.counter(sp8);
        sp8.print();
        
        TStream<Double> sp9 =splits1.get(9).tag("split","sp9");
        sp9 = Metrics.counter(sp9);
        sp9.print();
        
        TStream<Double> sp10 =splits1.get(10).tag("split","sp10");
        sp10 = Metrics.counter(sp10);
        sp10.print();
        
        
        // Alternative 'split' functionality using 10 filters, then compare the results if possible.
        TStream<Double>  filter10 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '-' ).tag("split-");
        TStream<Double>  filter0 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '0' ).tag("split0");
        TStream<Double>  filter1 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '1' ).tag("split1");
        TStream<Double>  filter2 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '2' ).tag("split2");
        TStream<Double>  filter3 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '3' ).tag("split3");
        TStream<Double>  filter4 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '4' ).tag("split4");
        TStream<Double>  filter5 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '5' ).tag("split5");
        TStream<Double>  filter6 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '6' ).tag("split6");
        TStream<Double>  filter7 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '7' ).tag("split7");
        TStream<Double>  filter8 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '8' ).tag("split8");
        TStream<Double>  filter9 = gaussian.filter(tuple -> tuple.toString().charAt(0) == '9' ).tag("split9");

        filter0 = filter0.peek(g -> System.out.println("filter0 : " + g));
        filter1 = filter1.peek(g -> System.out.println("filter1 : " + g));
        filter2 = filter2.peek(g -> System.out.println("filter2 : " + g));
        filter3 = filter3.peek(g -> System.out.println("filter3 : " + g));
        filter4 = filter4.peek(g -> System.out.println("filter4 : " + g));
        filter5 = filter5.peek(g -> System.out.println("filter5 : " + g));
        filter6 = filter6.peek(g -> System.out.println("filter6 : " + g));
        filter7 = filter7.peek(g -> System.out.println("filter7 : " + g));
        filter8 = filter8.peek(g -> System.out.println("filter8 : " + g));
        filter9 = filter9.peek(g -> System.out.println("filter9 : " + g));
        filter10 = filter10.peek(g -> System.out.println("filter10 : " + g));

        filter0.print();
        filter1.print();
        filter2.print();
        filter3.print();
        filter4.print();
        filter5.print();
        filter6.print();
        filter7.print();
        filter8.print();
        filter9.print();
        filter10.print();

        //**************************************************************	   
        //Source 2 using complex tuple type
        //**************************************************************

        Random r2 = new Random();
        TStream<MyClass1> mc1 = t.poll(
                () -> new MyClass1(Double.toString(r2.nextGaussian()), 					                                           
                        Double.toString(r2.nextGaussian()),r1.nextGaussian()
                ),100, TimeUnit.MILLISECONDS).tag("mc1");

        mc1.peek(g -> System.out.print(g.toString()));

        mc1.modify(tuple -> new MyClass1(tuple.getS1() + "a1 b1 c1 d1 ", tuple.getS2() +" e1 f1 g1 h1 ", tuple.getD1() +1) );
        mc1.peek(tuple -> System.out.println("MyClass1: " + tuple.toString()));

        mc1.flatMap(tuple -> Arrays.asList(tuple.toString().split(" ")));

        //An asString

        TStream<String> mcs1 = mc1.asString().tag("mcs1");
        mcs1.peek(tuple -> System.out.println(" mcs1_source2: " + tuple.toString()));

        List<TStream<String>> splits2 = mcs1.split(2, tuple -> {
            switch (tuple.toString().charAt(0)) {
            case '-':              //negative numbers
                return 0;
            default:               //everything else
                return 1;
            }
        });

        TStream<String> s2_0 = splits2.get(0).tag("s2_0");
        TStream<String> s2_1 = splits2.get(1).tag("s2_1");
        s2_0.sink(tuple -> System.out.println("s2_0: " + tuple.toString()));
        s2_1.sink(tuple -> System.out.println("s2_1: " + tuple.toString()));

        //**************************************************************	   
        // Source 3 - nested complex type
        //**************************************************************	   
        Random r3 = new Random();
        TStream<MyClass2> mc2 = t.poll(
                () -> new MyClass2(
                        new MyClass1(
                                Double.toString(r3.nextGaussian()), 					                                           
                                Double.toString(r3.nextGaussian()),
                                r3.nextGaussian()), 
                        new MyClass1(
                                Double.toString(r3.nextGaussian()), 
                                Double.toString(r3.nextGaussian()),
                                r3.nextGaussian()),
                        r3.nextGaussian(),
                        Double.toString(r3.nextGaussian())
                ), 100, TimeUnit.MILLISECONDS).tag("mc2");
        
        // testing Peek!
        mc2 = mc2.peek(tuple -> System.out.println("MyClass2_source3:" + tuple.toString()));

        // A filter
        mc2 = mc2.filter(tuple -> 
        ( tuple.getMc1().getD1() > .5 && tuple.getMc2().getD1() < -.5 ) 
        ||
        ! tuple.getS1().startsWith("abc"));

        // modify
        mc2 = mc2.modify(
                tuple -> new MyClass2(
                            new MyClass1(tuple.getMc1().getS1() + " c3 d3 e3 f3 ", 
                                tuple.getMc2().getS2() + "g3 h3 i3 j3 ",
                                tuple.getMc1().getD1() -13.3333),
                            new MyClass1(tuple.getMc2().getS2() + " x31 x32 x33 x34 x35 ", 
                                tuple.getMc1().getS2() + " y31 y32 y33 y34 y35 ",
                                tuple.getMc2().getD1() +13.33),
                        tuple.getD1() *2 -.04556,
                        tuple.getS1() + " a31 b32 c33 d34 e35 "
                        )  
                ).tag("mc2.modify");

        //**************************************************************	   
        // Source 4: Clone of Source 3 for now to generate more vertices. 
        //**************************************************************	
        Random r4 = new Random();
        TStream<MyClass2> mc4 = t.poll(
                () -> new MyClass2(
                        new MyClass1(
                                Double.toString(r4.nextGaussian()), 					                                           
                                Double.toString(r4.nextGaussian()),
                                r4.nextGaussian()), 
                        new MyClass1(Double.toString(r4.nextGaussian()), 
                                Double.toString(r4.nextGaussian()),
                                r4.nextGaussian()),
                        r4.nextGaussian(),
                        Double.toString(r4.nextGaussian())
                ), 100, TimeUnit.MILLISECONDS).tag("mc4");
        
        // testing Peek!
        mc4 = mc4.peek(tuple -> System.out.println("MyClass2_source4:" + tuple.toString()));

        // A filter
        mc4 = mc4.filter(tuple -> 
        ( tuple.getMc1().getD1() > .5 && tuple.getMc2().getD1() < -.5 ) 
        ||
        ! tuple.getS1().startsWith("abc"));

        // modify
        mc4 = mc4.modify(
                tuple -> new MyClass2(
                        new MyClass1(tuple.getMc1().getS1() + " c41 d42 d43 f44 ", 
                                tuple.getMc2().getS2() + "g41 h42 i43 j44 ",
                                tuple.getMc1().getD1() -17.03),
                        new MyClass1(tuple.getMc2().getS2() + " x41 x42 x43 x44 x45 ", 
                                tuple.getMc1().getS2() + " y41 y42 y43 y44 y45 ",
                                tuple.getMc2().getD1() +14.4444),
                        tuple.getD1() *2 -.04556,
                        tuple.getS1() + " a b c d e "
                        )  
                );        

        //Try to union mc4 to mc4
        TStream<MyClass2> su1 = mc4.union(new HashSet<>(Arrays.asList(mc2, mc4)));
        su1.print();

        //**************************************************************	   
        // Source 5: Clone of Source 3 for now to generate more vertices. 
        //**************************************************************
        Random r5 = new Random();
        TStream<MyClass2> mc5 = t2.poll(
                () -> new MyClass2(
                        new MyClass1(
                                Double.toString(r5.nextGaussian()), 					                                           
                                Double.toString(r5.nextGaussian()),
                                r5.nextGaussian()), 
                        new MyClass1(Double.toString(r5.nextGaussian()), 
                                Double.toString(r5.nextGaussian()),
                                r5.nextGaussian()),
                        r5.nextGaussian(),
                        Double.toString(r5.nextGaussian())
                        ), 100, TimeUnit.MILLISECONDS);
        
        // Add a counter
        mc5 = Metrics.counter(mc5);
        
        // testing Peek!
        mc5 = mc5.peek(tuple -> System.out.println("MyClass2_source5:" + tuple.toString()));

        // A filter
        mc5 = mc5.filter(tuple -> 
        ( tuple.getMc1().getD1() > .5 && tuple.getMc2().getD1() < -.5 ) 
        ||
        ! tuple.getS1().startsWith("abc"));

        // modify
        mc5 = mc5.modify(
                tuple -> new MyClass2(
                        new MyClass1(tuple.getMc1().getS1() + " c51 d52 e53 f54 ", 
                                tuple.getMc2().getS2() + "g51 h52 i53 j54 ",
                                tuple.getMc1().getD1() -17.03),
                        new MyClass1(tuple.getMc2().getS2() + " x51 x52 x53 x54 x55 ", 
                                tuple.getMc1().getS2() + " y51 y52 y53 y54 y55 ",
                                tuple.getMc2().getD1() +15.555),
                        tuple.getD1() *2 -.04556,
                        tuple.getS1() + " a51 b52 c53 d54 e55 ")
                ).tag("mc5");

        
        mc5.sink(tuple -> {});
 
        // Submit the jobs
        tp.submit(t);
        tp.submit(t2);

        
        // If the console option was specified, write the console URL into file consoleUrl.txt
        if (console) {
            try {
                PrintWriter writer = new PrintWriter("consoleUrl.txt", "UTF-8");
                writer.println(tp.getServices().getService(HttpServer.class).getConsoleUrl());
                writer.close();
            } catch ( Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("TopologyTestBasic end");
       
    }
    
}
