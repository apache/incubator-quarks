/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quarks.samples.topology;

import quarks.console.server.HttpServer;
import quarks.function.ToIntFunction;
import quarks.providers.development.DevelopmentProvider;
import quarks.topology.TStream;
import quarks.topology.Topology;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SplitWithEnumSample {

  private enum EvenNumberEnum {

    ZERO(0), TWO(2), FOUR(4), SIX(6), EIGHT(8), TEN(10);

    public int num;

    EvenNumberEnum(int input) {
      this.num = input;
    }

    public int getValue() {
      return num;
    }

  }

  public static void main(String[] args) throws Exception {
    DevelopmentProvider dtp = new DevelopmentProvider();

    Topology t = dtp.newTopology("SplitWithEnumSample");

    Random r = new Random();

    TStream<Integer> d = t
        .poll(() -> r.nextInt(20), 500, TimeUnit.MILLISECONDS);

    List<TStream<Integer>> categories = d.split(EvenNumberEnum.class, new ToIntFunction<Integer>() {
      @Override
      public int applyAsInt(Integer inputNum) {
        for (EvenNumberEnum enumVal : EvenNumberEnum.values()) {
          if (enumVal.getValue() == inputNum) {
            return enumVal.ordinal();
          }
        }
        return -1;
      }
    });

    for (TStream<Integer> enumStream : categories.subList(0, categories.size())) {
      enumStream.sink(tuple -> System.out.println("DataInEnum = " + tuple));
    }

    dtp.submit(t);

    System.out.println(
        dtp.getServices().getService(HttpServer.class).getConsoleUrl());
  }
}
