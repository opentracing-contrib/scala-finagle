/*
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.finagle

import java.util

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.{Future, Return, Throw}
import io.opentracing.propagation.Format
import io.opentracing.tag.Tags
import io.opentracing.{References, Tracer}

/**
  * OpenTracing Http filter
  *
  * @param tracer         OpenTracing tracer
  * @param isServerFilter true if filter is applied to Server, false if applied to Client
  */
class OpenTracingHttpFilter(tracer: Tracer, isServerFilter: Boolean) extends SimpleFilter[Request, Response] {

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {

    val spanBuilder = tracer.buildSpan(request.method.name)
      .withTag(Tags.COMPONENT.getKey, "finagle")
      .withTag(Tags.HTTP_METHOD.getKey, request.method.name)
      .withTag(Tags.HTTP_URL.getKey, request.uri)

    if (isServerFilter) {
      spanBuilder.withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_SERVER)
    } else {
      spanBuilder.withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_CLIENT)
    }

    if (request.remotePort > 0) {
      spanBuilder.withTag(Tags.PEER_PORT.getKey, request.remotePort)
      spanBuilder.withTag(Tags.PEER_HOSTNAME.getKey, request.remoteHost)
    }

    val parent = tracer.extract(Format.Builtin.HTTP_HEADERS,
      new HeaderMapExtractAdapter(request.headerMap))
    if (parent != null) {
      spanBuilder.addReference(References.FOLLOWS_FROM, parent)
    }

    val span = spanBuilder.startManual()

    tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
      new HeaderMapInjectAdapter(request.headerMap))

    val future = service(request)

    future respond {
      case Return(reply) =>
        span.setTag(Tags.HTTP_STATUS.getKey, reply.status.code)
        span.finish()
      case Throw(throwable) =>
        val exceptionLogs: util.Map[String, AnyRef] = new util.LinkedHashMap[String, AnyRef](2)
        exceptionLogs.put("event", Tags.ERROR.getKey)
        exceptionLogs.put("error.object", throwable)
        span.log(exceptionLogs)
        span.setTag(Tags.ERROR.getKey, true)
        span.finish()
    }

    future
  }
}
