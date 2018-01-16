/*
 * Copyright 2017-2018 The OpenTracing Authors
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

import java.util.concurrent.{Callable, TimeUnit}

import com.twitter.finagle.http.Response
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}
import io.opentracing.mock.MockTracer
import io.opentracing.mock.MockTracer.Propagator
import io.opentracing.util.ThreadLocalScopeManager
import org.awaitility.Awaitility.await
import org.hamcrest.core.IsEqual.equalTo
import org.scalatest.FunSuite


class OpenTracingTest extends FunSuite {

  val mockTracer = new MockTracer(new ThreadLocalScopeManager, Propagator.TEXT_MAP)
  val port = ":53732"

  test("test instrumentation") {
    mockTracer.reset()

    val service = new OpenTracingHttpFilter(mockTracer, true) andThen new Service[http.Request, http.Response] {
      def apply(req: http.Request): Future[http.Response] = {

        val response = Response()
        response.setContentString("Hello from Finagle\n")
        Future.value(response)
      }
    }

    val server = Http.server.serve(port, service)

    val client = new OpenTracingHttpFilter(mockTracer, false) andThen Http.client.newService(port)
    val request = http.Request(http.Method.Get, "/")

    val responseFuture: Future[http.Response] = client(request)

    val result = Await.result(responseFuture)
    println(result + " " + result.contentString)

    await atMost(15, TimeUnit.SECONDS) until(reportedSpansSize(mockTracer), equalTo(2))

    val spans = mockTracer.finishedSpans()
    assert(spans.size() == 2)

    spans.forEach(span => {
      assert(span.operationName() == "GET")
      assert(span.context().traceId() == 1)
    })

    server.close()
  }

  private def reportedSpansSize(mockTracer: MockTracer): Callable[Int] = {
    () => mockTracer.finishedSpans().size()
  }
}

