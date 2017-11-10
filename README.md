[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Finagle Instrumentation
OpenTracing instrumentation for Finagle.

## Installation

build.sbt
```sbt
libraryDependencies += "io.opentracing.contrib" % "opentracing-finagle" % "0.0.1"
```

## Usage
 
```scala
// Instantiate tracer
val tracer: Tracer = ...
```

### Http Server
```scala
// Apply OpenTracingHttpFilter to Finagle service to serve HTTP requests
val service = new OpenTracingHttpFilter(tracer) andThen new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] =
      Future.value(
        http.Response(req.version, http.Status.Ok)
      )
  }
  
// Create server   
val server = Http.server.serve(":8080", service)
Await.ready(server)
```

### Http Client
```scala
// Apply OpenTracingHttpFilter to Finagle client service 
val client = new OpenTracingHttpFilter(tracer) andThen Http.client.newService(":8080")

// Build request
val request = http.Request(http.Method.Get, "/")

// Build response
val response: Future[http.Response] = client(request)

// Wait for result
val result = Await.result(response)
```


[ci-img]: https://travis-ci.org/opentracing-contrib/scala-finagle.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/scala-finagle
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-finagle.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-finagle
