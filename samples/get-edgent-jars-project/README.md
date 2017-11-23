<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

The `get-edgent-jars-project` can be used to copy Apache Edgent jars
and their transitive dependencies from a local or remote maven 
repository into bundles under `target`.

Use `get-edgent-jars.sh` to create bundles containing the jars.
The script also creates `target/classpath.sh` for composing a classpath
to use the Edgent jars.

By default the script retrieves the Edgent java8 platform jars for the
project's default Edgent version.

``` sh
cd get-edgent-jars-project
./get-edgent-jars.sh --version 1.3.0-SNAPSHOT  # retrieve the Edgent 1.3.0-SNAPSHOT java8 jars
##### Generating dependency decls...
##### Generating pom.xml...
...
##### Generating the bundles...
...
##### Generating classpath.sh...
##### Bundle LICENSING information:
...
##### Using a bundle:

    copy a bundle from target and unpack it
    copy target/classpath.sh and use it to compose a classpath:

        export CLASSPATH=`./classpath.sh --add-slf4j-jdk <path-to-unpacked-bundle>`

    Omit "--add-slf4j-jdk" to omit an slf4j-jdk* implementation jar from the classpath.
```


For more usage information:
``` sh
get-edgent-jars.sh -h
```
