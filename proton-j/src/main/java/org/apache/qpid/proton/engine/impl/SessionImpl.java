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

import java.util.*;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Session;

public class SessionImpl extends EndpointImpl implements Session
{
    private final ConnectionImpl _connection;

    private Map<String, SenderImpl> _senders = new LinkedHashMap<String, SenderImpl>();
    private Map<String, ReceiverImpl>  _receivers = new LinkedHashMap<String, ReceiverImpl>();
    private TransportSession _transportSession;

    private LinkNode<SessionImpl> _node;


    public SessionImpl(ConnectionImpl connection)
    {
        _connection = connection;
        _node = _connection.addSessionEndpoint(this);
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

    public SenderImpl sender(String name)
    {
        SenderImpl sender = _senders.get(name);
        if(sender == null)
        {
            sender = new SenderImpl(this, name);
            _senders.put(name, sender);
        }
        return sender;
    }

    public ReceiverImpl receiver(String name)
    {
        ReceiverImpl receiver = _receivers.get(name);
        if(receiver == null)
        {
            receiver = new ReceiverImpl(this, name);
            _receivers.put(name, receiver);
        }
        return receiver;
    }

    public Session next(EnumSet<EndpointState> local, EnumSet<EndpointState> remote)
    {
        LinkNode.Query<SessionImpl> query = new EndpointImplQuery<SessionImpl>(local, remote);

        LinkNode<SessionImpl> sessionNode = _node.next(query);

        return sessionNode == null ? null : sessionNode.getValue();
    }

    @Override
    protected ConnectionImpl getConnectionImpl()
    {
        return _connection;
    }

    public ConnectionImpl getConnection()
    {
        return getConnectionImpl();
    }

    public void free()
    {
        super.free();

        _connection.removeSessionEndpoint(_node);
        _node = null;

        for(SenderImpl sender : _senders.values())
        {
            sender.free();
        }
        _senders.clear();
        for(ReceiverImpl receiver : _receivers.values())
        {
            receiver.free();
        }
        _receivers.clear();
    }

    TransportSession getTransportSession()
    {
        return _transportSession;
    }

    void setTransportSession(TransportSession transportSession)
    {
        _transportSession = transportSession;
    }

    void setNode(LinkNode<SessionImpl> node)
    {
        _node = node;
    }

    void freeSender(SenderImpl sender)
    {
        _senders.remove(sender.getName());
    }

    void freeReceiver(ReceiverImpl receiver)
    {
        _receivers.remove(receiver.getName());
    }

    public boolean clearIncomingWindowResize()
    {
        return getTransportSession().clearIncomingWindowResize();
    }
}
