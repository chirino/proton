
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


package org.apache.qpid.proton.type.messaging;
import java.util.Map;


import org.apache.qpid.proton.codec.Decoder;
import org.apache.qpid.proton.codec.DescribedTypeConstructor;
import org.apache.qpid.proton.type.*;


public class MessageAnnotations
      implements DescribedType , Section
{
    private static final Object[] DESCRIPTORS =
    {
        UnsignedLong.valueOf(0x0000000000000072L), Symbol.valueOf("amqp:message-annotations:map"), 
    };

    private static final UnsignedLong DESCRIPTOR = UnsignedLong.valueOf(0x0000000000000072L);
    private final Map _value;

    public MessageAnnotations(Map value)
    {
        _value = value;
    }

    public Map getValue()
    {
        return _value;
    }

    public Object getDescriptor()
    {
        return DESCRIPTOR;
    }

    public Object getDescribed()
    {
        return _value;
    }

    private static class MessageAnnotationsConstructor implements DescribedTypeConstructor<MessageAnnotations>
    {
        public MessageAnnotations newInstance(Object described)
        {
            return new MessageAnnotations( (Map) described );
        }

        public Class<MessageAnnotations> getTypeClass()
        {
            return MessageAnnotations.class;
        }
    }
      

    public static void register(Decoder decoder)
    {
        MessageAnnotationsConstructor constructor = new MessageAnnotationsConstructor();
        for(Object descriptor : DESCRIPTORS)
        {
            decoder.register(descriptor, constructor);
        }
    }
}
  