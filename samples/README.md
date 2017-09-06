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

See [APPLICATION_DEVELOPMENT.md](APPLICATION_DEVELOPMENT.md) for general
information on Edgent Application Development, Packaging and Execution.

#Building the Edgent samples

By default java8 class files are generated.
Java7 and Android platform class files are produced when the appropriate
profile is specified.

```sh
./mvnw clean install  # -Pplatform-java7 or -Pplatform-android as needed
```

A standard jar and uber jar is created for each sample category
in the sample category's target directory: `<category>/target`.


##Running the samples

See the README.md in each sample category directory.
