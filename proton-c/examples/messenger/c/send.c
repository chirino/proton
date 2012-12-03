/*
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

#include "proton/message.h"
#include "proton/messenger.h"

#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#define check(messenger)                                         \
  {                                                              \
    if(pn_messenger_errno(messenger))                            \
    {                                                            \
      die(__FILE__, __LINE__, pn_messenger_error(messenger));    \
    }                                                            \
  }                                                              \

void die(const char *file, int line, const char *message)
{
  fprintf(stderr, "%s:%i: %s\n", file, line, message);
  exit(1);
}

void usage()
{
  printf("Usage: send [-a addr] [message]\n");
  printf("-a     \tThe target address [amqp[s]://domain[/name]]\n");
  printf("message\tA text string to send.\n");
  exit(0);
}

int main(int argc, char** argv)
{
  int c;
  opterr = 0;
  char * address = "amqp://0.0.0.0";
  char * msgtext = "Hello World!";

  while((c = getopt(argc, argv, "ha:b:c:")) != -1)
  {
    switch(c)
    {
    case 'a': address = optarg; break;
    case 'h': usage(); break;

    case '?':
      if(optopt == 'a')
      {
        fprintf(stderr, "Option -%c requires an argument.\n", optopt);
      }
      else if(isprint(optopt))
      {
        fprintf(stderr, "Unknown option `-%c'.\n", optopt);
      }
      else
      {
        fprintf(stderr, "Unknown option character `\\x%x'.\n", optopt);
      }
      return 1;
    default:
      abort();
    }
  }

  if((argc - optind) > 0) msgtext = argv[argc - optind];

  pn_message_t * message;
  pn_messenger_t * messenger;

  message = pn_message();
  messenger = pn_messenger(NULL);

  pn_messenger_start(messenger);

  pn_message_set_address(message, address);
  pn_data_t *body = pn_message_body(message);
  pn_data_put_string(body, pn_bytes(strlen(msgtext), msgtext));
  pn_messenger_put(messenger, message);
  check(messenger);
  pn_messenger_send(messenger);
  check(messenger);

  pn_messenger_stop(messenger);
  pn_messenger_free(messenger);
  pn_message_free(message);

  return 0;
}
