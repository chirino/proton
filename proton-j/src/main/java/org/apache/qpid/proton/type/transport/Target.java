package org.apache.qpid.proton.type.transport;

import org.apache.qpid.proton.type.Symbol;
import org.apache.qpid.proton.type.UnsignedInteger;

import java.util.Map;

public interface Target
{
    Target copy();

    Symbol[] getCapabilities();

    Object getDescriptor();
}
