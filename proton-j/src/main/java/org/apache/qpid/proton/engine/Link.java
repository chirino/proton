/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.proton.engine;

import org.apache.qpid.proton.type.transport.Source;
import org.apache.qpid.proton.type.transport.Target;

import java.util.EnumSet;

/**
 * Link
 *
 */

public interface Link extends Endpoint
{

    /**
     *
     * @param tag a tag for the delivery
     * @param offset
     *@param length @return a Delivery object
     */
    public Delivery delivery(byte[] tag, int offset, int length);

    /**
     * @return the unsettled deliveries for this link
     */
    public Sequence<Delivery> unsettled();

    /**
     * @return return the current delivery
     */
    Delivery current();

    /**
     * Attempts to advance the current delivery
     * @return true if it can advance, false if it cannot
     */
    boolean advance();

    Source getLocalSource();
    Target getLocalTarget();
    void setLocalSource(Source address);
    void setLocalTarget(Target address);

    Source getRemoteSource();
    Target getRemoteTarget();


    public Link next(EnumSet<EndpointState> local, EnumSet<EndpointState> remote);

    public int getCredit();
    public int getQueued();
    public int getUnsettled();

    public Session getSession();

}
