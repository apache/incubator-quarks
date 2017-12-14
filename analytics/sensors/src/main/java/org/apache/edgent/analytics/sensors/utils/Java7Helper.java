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
package org.apache.edgent.analytics.sensors.utils;

public class Java7Helper {

    public static int byteToUnsignedInt(byte input) {
        return ((int) input) & 0xff;
    }

    public static int shortToUnsignedInt(short input) {
        return ((int) input) & 0xffff;
    }

    public static long intToUnsignedLong(int input) {
        return ((long) input) & 0xffffffffL;
    }

    public static String intToUnsignedString(int input) {
        return Long.toString(intToUnsignedLong(input));
    }

    public static String longToUnsignedString(long input) {
        // In case of a positive signed value, just output that.
        if (input >= 0) {
            return Long.toString(input, 10);
        }
        // If the value is negative, the most significant bit is
        // 1 and Java is interpreting the number as negative value.
        else {
            // Shift everything right one bit (filling up with 0)
            long quot = (input >>> 1) / 5;
            long rem = input - quot * 10;
            return Long.toString(quot) + rem;
        }
    }

    public static int intCompareUnsigned(int x, int y) {
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    public static int longCompareUnsigned(long x, long y) {
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

}
