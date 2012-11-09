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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <proton/framing.h>
#include <proton/engine.h>
#include <proton/buffer.h>
#include "dispatcher.h"
#include "protocol.h"
#include "../util.h"

pn_dispatcher_t *pn_dispatcher(uint8_t frame_type, void *context)
{
  pn_dispatcher_t *disp = (pn_dispatcher_t *) calloc(sizeof(pn_dispatcher_t), 1);

  disp->frame_type = frame_type;
  disp->context = context;
  disp->trace = PN_TRACE_OFF;

  disp->input = pn_buffer(1024);

  disp->channel = 0;
  disp->code = 0;
  disp->args = pn_data(16);
  disp->payload = NULL;
  disp->size = 0;

  disp->output_args = pn_data(16);
  disp->frame = pn_buffer( 4*1024 );
  // XXX
  disp->capacity = 4*1024;
  disp->output = (char *) malloc(disp->capacity);
  disp->available = 0;

  disp->halt = false;
  disp->batch = true;

  return disp;
}

void pn_dispatcher_free(pn_dispatcher_t *disp)
{
  if (disp) {
    pn_buffer_free(disp->input);
    pn_data_free(disp->args);
    pn_data_free(disp->output_args);
    pn_buffer_free(disp->frame);
    free(disp->output);
    free(disp);
  }
}

void pn_dispatcher_action(pn_dispatcher_t *disp, uint8_t code, const char *name,
                          pn_action_t *action)
{
  disp->actions[code] = action;
  disp->names[code] = name;
}

typedef enum {IN, OUT} pn_dir_t;

static void pn_do_trace(pn_dispatcher_t *disp, uint16_t ch, pn_dir_t dir,
                        pn_data_t *args, const char *payload, size_t size)
{
  if (disp->trace & PN_TRACE_FRM) {
    uint64_t code64;
    bool scanned;
    pn_data_scan(args, "D?L.", &scanned, &code64);
    uint8_t code = scanned ? code64 : 0;
    size_t n = SCRATCH;
    pn_data_format(args, disp->scratch, &n);
    pn_dispatcher_trace(disp, ch, "%s %s %s", dir == OUT ? "->" : "<-",
                        disp->names[code], disp->scratch);
    if (size) {
      char buf[1024];
      int e = pn_quote_data(buf, 1024, payload, size);
      fprintf(stderr, " (%zu) \"%s\"%s\n", size, buf,
              e == PN_OVERFLOW ? "... (truncated)" : "");
    } else {
      fprintf(stderr, "\n");
    }
  }
}

void pn_dispatcher_trace(pn_dispatcher_t *disp, uint16_t ch, char *fmt, ...)
{
  va_list ap;
  fprintf(stderr, "[%p:%u] ", (void *) disp, ch);

  va_start(ap, fmt);
  vfprintf(stderr, fmt, ap);
  va_end(ap);
}

int pn_dispatch_frame(pn_dispatcher_t *disp, pn_frame_t frame)
{
  ssize_t dsize = pn_data_decode(disp->args, frame.payload, frame.size);
  if (dsize < 0) {
    fprintf(stderr, "Error decoding frame: %s %s\n", pn_code(dsize),
            pn_data_error(disp->args));
    pn_fprint_data(stderr, frame.payload, frame.size);
    fprintf(stderr, "\n");
    return dsize;
  }

  disp->channel = frame.channel;
  // XXX: assuming numeric
  uint64_t lcode;
  bool scanned;
  int e = pn_data_scan(disp->args, "D?L.", &scanned, &lcode);
  if (e) {
    fprintf(stderr, "Scan error\n");
    return e;
  }
  if (!scanned) {
    fprintf(stderr, "Error dispatching frame\n");
    return PN_ERR;
  }
  uint8_t code = lcode;
  disp->code = code;
  disp->size = frame.size - dsize;
  if (disp->size)
    disp->payload = frame.payload + dsize;

  pn_do_trace(disp, disp->channel, IN, disp->args, disp->payload, disp->size);

  pn_action_t *action = disp->actions[code];
  int err = action(disp);

  disp->channel = 0;
  disp->code = 0;
  pn_data_clear(disp->args);
  disp->size = 0;
  disp->payload = NULL;

  return err;
}

ssize_t pn_dispatcher_input(pn_dispatcher_t *disp, const char *bytes, size_t available)
{
  size_t leftover = pn_buffer_size(disp->input);
  if (leftover) {
    int e = pn_buffer_append(disp->input, bytes, available);
    if (e) return e;
    pn_bytes_t b = pn_buffer_bytes(disp->input);
    bytes = b.start;
    available = b.size;
  }

  size_t read = 0;

  while (!disp->halt) {
    pn_frame_t frame;

    size_t n = pn_read_frame(&frame, bytes + read, available - read);
    if (n) {
      int e = pn_dispatch_frame(disp, frame);
      if (e) return e;
      read += n;
    } else {
      if (leftover) {
        pn_buffer_trim(disp->input, read, 0);
      } else {
        int e = pn_buffer_append(disp->input, bytes + read, available - read);
        if (e) return e;
      }
      read = available;
      break;
    }

    if (!disp->batch) break;
  }

  return read - leftover;
}

int pn_scan_args(pn_dispatcher_t *disp, const char *fmt, ...)
{
  va_list ap;
  va_start(ap, fmt);
  int err = pn_data_vscan(disp->args, fmt, ap);
  va_end(ap);
  if (err) printf("scan error: %s\n", fmt);
  return err;
}

void pn_set_payload(pn_dispatcher_t *disp, const char *data, size_t size)
{
  disp->output_payload = data;
  disp->output_size = size;
}

int pn_post_frame(pn_dispatcher_t *disp, uint16_t ch, const char *fmt, ...)
{
  va_list ap;
  va_start(ap, fmt);
  pn_data_clear(disp->output_args);
  int err = pn_data_vfill(disp->output_args, fmt, ap);
  va_end(ap);
  if (err) {
    fprintf(stderr, "error posting frame: %s, %s: %s\n", fmt, pn_code(err), pn_data_error(disp->output_args));
    return PN_ERR;
  }

  pn_do_trace(disp, ch, OUT, disp->output_args, disp->output_payload, disp->output_size);

 encode_performatives:
  pn_buffer_clear( disp->frame );
  pn_bytes_t buf = pn_buffer_bytes( disp->frame );
  buf.size = pn_buffer_available( disp->frame );

  ssize_t wr = pn_data_encode( disp->output_args, buf.start, buf.size );
  if (wr < 0) {
    if (wr == PN_OVERFLOW) {
      pn_buffer_ensure( disp->frame, pn_buffer_available( disp->frame ) * 2 );
      goto encode_performatives;
    }
    fprintf(stderr, "error posting frame: %s", pn_code(wr));
    return PN_ERR;
  }

  pn_frame_t frame = {disp->frame_type};
  frame.channel = ch;
  frame.payload = buf.start;
  frame.size = wr;
  size_t n;
  while (!(n = pn_write_frame(disp->output + disp->available,
                              disp->capacity - disp->available, frame))) {
    disp->capacity *= 2;
    disp->output = (char *) realloc(disp->output, disp->capacity);
  }
  if (disp->trace & PN_TRACE_RAW) {
    fprintf(stderr, "RAW: \"");
    pn_fprint_data(stderr, disp->output + disp->available, n);
    fprintf(stderr, "\"\n");
  }
  disp->available += n;

  return 0;
}

ssize_t pn_dispatcher_output(pn_dispatcher_t *disp, char *bytes, size_t size)
{
  int n = disp->available < size ? disp->available : size;
  memmove(bytes, disp->output, n);
  memmove(disp->output, disp->output + n, disp->available - n);
  disp->available -= n;
  // XXX: need to check for errors
  return n;
}


int pn_post_transfer_frame(pn_dispatcher_t *disp, uint16_t ch,
                           uint32_t handle,
                           pn_sequence_t id,
                           const pn_bytes_t *tag,
                           uint32_t message_format,
                           bool settled,
                           bool more)
{
  bool more_flag = more;

  // create preformatives, assuming 'more' flag need not change

 compute_performatives:
  pn_data_clear(disp->output_args);
  int err = pn_data_fill(disp->output_args, "DL[IIzIoo]", TRANSFER,
                         handle, id, tag->size, tag->start,
                         message_format,
                         settled, more_flag);
  if (err) {
    fprintf(stderr, "error posting transfer frame: %s: %s\n", pn_code(err), pn_data_error(disp->output_args));
    return PN_ERR;
  }

  do { // send as many frames as possible without changing the 'more' flag...

  encode_performatives:
    pn_buffer_clear( disp->frame );
    pn_bytes_t buf = pn_buffer_bytes( disp->frame );
    buf.size = pn_buffer_available( disp->frame );

    ssize_t wr = pn_data_encode(disp->output_args, buf.start, buf.size);
    if (wr < 0) {
      if (wr == PN_OVERFLOW) {
        pn_buffer_ensure( disp->frame, pn_buffer_available( disp->frame ) * 2 );
        goto encode_performatives;
      }
      fprintf(stderr, "error posting frame: %s", pn_code(wr));
      return PN_ERR;
    }
    buf.size = wr;

    // check if we need to break up the outbound frame
    size_t available = disp->output_size;
    if (disp->remote_max_frame) {
      if ((available + buf.size) > disp->remote_max_frame - 8) {
        available = disp->remote_max_frame - 8 - buf.size;
        if (more_flag == false) {
          more_flag = true;
          goto compute_performatives;  // deal with flag change
        }
      } else if (more_flag == true && more == false) {
        // caller has no more, and this is the last frame
        more_flag = false;
        goto compute_performatives;
      }
    }

    if (pn_buffer_available( disp->frame ) < (available + buf.size)) {
      // not enough room for payload - try again...
      pn_buffer_ensure( disp->frame, available + buf.size );
      goto encode_performatives;
    }

    pn_do_trace(disp, ch, OUT, disp->output_args, disp->output_payload, disp->output_size);

    memmove( buf.start + buf.size, disp->output_payload, available);
    disp->output_payload += available;
    disp->output_size -= available;
    buf.size += available;

    pn_frame_t frame = {disp->frame_type};
    frame.channel = ch;
    frame.payload = buf.start;
    frame.size = buf.size;

    size_t n;
    while (!(n = pn_write_frame(disp->output + disp->available,
                                disp->capacity - disp->available, frame))) {
      disp->capacity *= 2;
      disp->output = realloc(disp->output, disp->capacity);
    }
    if (disp->trace & PN_TRACE_RAW) {
      fprintf(stderr, "RAW: \"");
      pn_fprint_data(stderr, disp->output + disp->available, n);
      fprintf(stderr, "\"\n");
    }
    disp->available += n;
  } while (disp->output_size > 0);

  disp->output_payload = NULL;
  return 0;
}

