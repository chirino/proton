#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

export PROTON_HOME=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

if [ -z "$CPROTON_BUILD" ]; then
    if [ -d $PROTON_HOME/proton-c/build ]; then
        PROTON_BINDINGS=$PROTON_HOME/proton-c/build/bindings
    else
        PROTON_BINDINGS=$PROTON_HOME/proton-c/bindings
    fi
else
    PROTON_BINDINGS=$CPROTON_BUILD/bindings
fi

# Python & Jython
export PYTHON_BINDINGS=$PROTON_BINDINGS/python
export COMMON_PYPATH=$PROTON_HOME/tests
export PYTHONPATH=$COMMON_PYPATH:$PROTON_HOME/proton-c/bindings/python:$PYTHON_BINDINGS
export JYTHONPATH=$COMMON_PYPATH:$PROTON_HOME/proton-j/src/main/scripts:$PROTON_HOME/proton-j/target/qpid-proton-1.0-SNAPSHOT.jar

# PHP
export PHP_BINDINGS=$PROTON_BINDINGS/php

# Ruby
export RUBY_BINDINGS=$PROTON_BINDINGS/ruby
export RUBYLIB=$RUBY_BINDINGS
