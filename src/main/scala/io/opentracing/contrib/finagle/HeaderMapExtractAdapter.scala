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

import com.twitter.finagle.http.HeaderMap
import io.opentracing.propagation.TextMap

class HeaderMapExtractAdapter(headerMap: HeaderMap) extends TextMap {

  override def put(key: String, value: String) = {
    throw new UnsupportedOperationException("HeaderMapExtractAdapter should only be used with Tracer.extract()")
  }

  override def iterator() = {
    import scala.collection.JavaConverters._
    headerMap.asJava.entrySet().iterator()
  }
}
