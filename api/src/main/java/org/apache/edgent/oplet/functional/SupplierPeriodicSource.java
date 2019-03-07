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
package org.apache.edgent.oplet.functional;

import static org.apache.edgent.function.Functions.closeFunction;

import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Supplier;
import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.core.PeriodicSource;

public class SupplierPeriodicSource<T> extends PeriodicSource<T> {

    private Supplier<T> data;

    public SupplierPeriodicSource(long period, TimeUnit unit, Supplier<T> data) {
        super(period, unit);
        this.data = data;
    }

    @Override
    public void initialize(OpletContext<Void, T> context) {
        super.initialize(context);
    }

    @Override
    public void close() throws Exception {
        closeFunction(data);
    }

    @Override
    public void fetchTuples() {
        T tuple = data.get();
        if (tuple != null)
            submit(tuple);
    }
}
