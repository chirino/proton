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

import java.util.EnumSet;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.type.transport.Source;
import org.apache.qpid.proton.type.transport.Target;

public abstract class LinkImpl extends EndpointImpl implements Link
{

    private final SessionImpl _session;

    DeliveryImpl _head;
    DeliveryImpl _tail;
    DeliveryImpl _current;
    private String _name;
    private Target _remoteTarget;
    private Target _localTarget;
    private Source _remoteSource;
    private Source _localSource;
    private int _queued;
    private int _credit;
    private int _unsettled;

    private final LinkNode<LinkImpl> _node;
    private boolean _drain;


    public LinkImpl(SessionImpl session, String name)
    {
        _session = session;
        _name = name;
        _node = session.getConnectionImpl().addLinkEndpoint(this);
    }


    public void open()
    {
        super.open();
        modified();
    }

    public void close()
    {
        super.close();
        modified();
    }

    String getName()
    {
        return _name;
    }



    public DeliveryImpl delivery(byte[] tag, int offset, int length)
    {

        incrementQueued();
        try
        {
        DeliveryImpl delivery = new DeliveryImpl(tag, this, _tail);
        if(_tail == null)
        {
            _head = delivery;
        }
        _tail = delivery;
        if(_current == null)
        {
            _current = delivery;
        }
        getConnectionImpl().workUpdate(delivery);
        return delivery;
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    public void free()
    {
        super.free();
        _session.getConnectionImpl().removeLinkEndpoint(_node);
        //TODO.
    }

    public void remove(DeliveryImpl delivery)
    {
        if(_head == delivery)
        {
            _head = delivery.getLinkNext();
        }
        if(_tail == delivery)
        {
            _tail = delivery.getLinkPrevious();
        }
        if(_current == delivery)
        {
            // TODO - what???
        }
    }

    public DeliveryImpl current()
    {
        return _current;
    }

    public boolean advance()
    {
        if(_current != null )
        {
            DeliveryImpl oldCurrent = _current;
            _current = _current.getLinkNext();
            getConnectionImpl().workUpdate(oldCurrent);

            if(_current != null)
            {
                getConnectionImpl().workUpdate(_current);
            }
            return true;
        }
        else
        {
            return false;
        }

    }

    @Override
    protected ConnectionImpl getConnectionImpl()
    {
        return _session.getConnectionImpl();
    }

    public SessionImpl getSession()
    {
        return _session;
    }

    public Link next(EnumSet<EndpointState> local, EnumSet<EndpointState> remote)
    {
        LinkNode.Query<LinkImpl> query = new EndpointImplQuery<LinkImpl>(local, remote);

        LinkNode<LinkImpl> linkNode = _node.next(query);

        return linkNode == null ? null : linkNode.getValue();

    }

    abstract TransportLink getTransportLink();

    abstract boolean workUpdate(DeliveryImpl delivery);

    public int getCredit()
    {
        return _credit;
    }

    public void addCredit(int credit)
    {
        _credit+=credit;
    }

    public void setCredit(int credit)
    {
        _credit = credit;
    }

    boolean hasCredit()
    {
        return _credit > 0;
    }

    void incrementCredit()
    {
        _credit++;
    }

    void decrementCredit()
    {
        _credit--;
    }

    public int getQueued()
    {
        return _queued;
    }

    void incrementQueued()
    {
        _queued++;
    }

    void decrementQueued()
    {
        _queued--;
    }

    public int getUnsettled()
    {
        return _unsettled;
    }

    void incrementUnsettled()
    {
        _unsettled++;
    }

    void decrementUnsettled()
    {
        _unsettled--;
    }

    void setDrain(boolean drain)
    {
        _drain = drain;
    }

    boolean getDrain()
    {
        return _drain;
    }

    public Target getRemoteTarget() {
        return copy(_remoteTarget);
    }

    public void setRemoteTarget(Target _remoteTarget) {
        this._remoteTarget = copy(_remoteTarget);
    }

    public Source getRemoteSource() {
        return copy(_remoteSource);
    }

    public void setRemoteSource(Source _remoteSource) {
        this._remoteSource = copy(_remoteSource);
    }

    public Target getLocalTarget() {
        return copy(_localTarget);
    }

    public void setLocalTarget(Target _localTarget) {
        this._localTarget = copy(_localTarget);
        modified();
    }

    public Source getLocalSource() {
        return copy(_localSource);
    }

    public void setLocalSource(Source _localSource) {
        this._localSource = copy(_localSource);
        modified();
    }

    private Target copy(Target other) {
        if( other == null ) {
            return null;
        } else {
            return other.copy();
        }
    }

    private Source copy(Source other) {
        if( other == null ) {
            return null;
        } else {
            return other.copy();
        }
    }

}
