/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.nordlander.a;

/**
 *      Need to grab stdout somehow during tests.
 *      This class makes that possible.
 */
public class ATestOutput implements AOutput{

    private static final String LN = System.getProperty("line.separator");

    StringBuffer sb = new StringBuffer();
    public void output(Object... args) {
        for(Object arg : args){
            sb.append(arg.toString());
            System.out.print(arg.toString());
        }
        sb.append(LN);
        System.out.println();
    }

    public String grab(){
        String ret = sb.toString();
        sb = new StringBuffer();
        return ret;
    }

    public String get(){
        return sb.toString();
    }
}