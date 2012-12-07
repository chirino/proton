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
package org.apache.qpid.proton.engine.impl;

import java.util.Iterator;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Sender;

public class SenderImpl  extends LinkImpl implements Sender
{
    private int _offered;
    private TransportSender _transportLink;
    private boolean _drained;

    public SenderImpl(SessionImpl session, String name)
    {
        super(session, name);
    }

    public void offer(final int credits)
    {
        _offered = credits;
    }

    public int send(final byte[] bytes, int offset, int length)
    {
        if( getLocalState() == EndpointState.CLOSED ) 
        {
            throw new IllegalStateException("send not allowed after the sender is closed.");
        }
        DeliveryImpl current = current();
        if(current == null || current.getLink() != this)
        {
            throw new IllegalArgumentException();//TODO.
        }
        return current.send(bytes, offset, length);
    }

    public void abort()
    {
        //TODO.
    }

    public Iterator<Delivery> unsettled()
    {
        return null;  //TODO.
    }


    public void free()
    {
        getSession().freeSender(this);
        super.free();

    }

    @Override
    public boolean advance()
    {
        DeliveryImpl delivery = current();

        boolean advance = super.advance();
        if(advance && _offered > 0)
        {
            _offered--;
        }
        if(advance)
        {
            decrementCredit();
            delivery.addToTransportWorkList();
        }

        return advance;
    }

    boolean hasOfferedCredits()
    {
        return _offered > 0;
    }

    @Override
    TransportSender getTransportLink()
    {
        return _transportLink;
    }

    void setTransportLink(TransportSender transportLink)
    {
        _transportLink = transportLink;
    }


    @Override
    boolean workUpdate(DeliveryImpl delivery)
    {
        return (delivery == current()) && hasCredit();
    }

    @Override
    public void setCredit(int credit)
    {
        super.setCredit(credit);
       /* while(getQueued()>0 && getCredit()>0)
        {
            advance();
        }*/
    }

    public void drained()
    {
        if(getDrain())
        {
            _drained = true;
            setCredit(0);
            modified();
        }
    }

    boolean clearDrained()
    {
        final boolean drained = _drained;
        if(drained)
        {
            _drained = false;
        }
        return drained;
    }
}
