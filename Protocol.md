# Protocol

This document describes the communication protocol between clients and the server.
All messages must be encoded in JSON format.

## Authentication

The first action a client must take is to authenticate itself and obtain a bearer token.  
The server expects the correct access token in the authorization header field.  
If authentication is not enabled (the `ACCESSTOKEN` environment variable is blank)
the server will accept any value as a valid access token.

```
CLIENT -> GET/auth
SERVER -> String
```

This bearer token is used to identify each client,
must be sent with each request in the authorization header field and once through the WebSocket.

```
CLIENT -> WS/: WebSocketAuth(bearerToken: String)
SERVER -> WebSocketAuthResponse(success: Boolean)
```

Once a WebSocket has been successfully associated with a client,
the client will receive updates on the status of jobs through it.  
If a client is subscribed to a particular job, more detailed updates are also provided.  
See [Job Update](#job-update) and [Detailed Job Update](#detailed-job-update) for more details.

### Examples

TODO

## Get Job list

A client can get the list of all jobs via a simple GET request.

```
CLIENT -> GET/job
SERVER -> List<SimJob>
```

### Examples

TODO

## Get Job

A client can retrieve a single job by its ID.

```
CLIENT -> GET/job/<ID>
SERVER -> SimJob
```

### Examples

TODO

## New Job

A new job can be submitted using a POST request.

```
CLIENT -> POST/job: JobNew(configs: List<StepConfig>, top: String, dat: String, forces: String)
SERVER -> SimJob
```

### Examples

TODO

## Delete Job

A job can be deleted by its ID using a DELETE request.

```
CLIENT -> DELETE/job/<ID>
```

### Examples

TODO

## Cancel Job

A job can be canceled by its ID using a PATCH request.

```
CLIENT -> PATCH/job/<ID>
```

### Examples

TODO

## Unsubscribe

If a client doesn't want to receive detailed updates anymore, they can unsubscribe with a POST request.  
Normal job updates will still be delivered.

```
CLIENT -> POST/job/unsubscribe
```

### Examples

TODO

## Subscribe Job

If a client wants to receive detailed updates for a job, they can subscribe with a POST request.  
Normal job updates will still be delivered.

```
CLIENT -> POST/job/subscribe/<ID>
```

### Examples

TODO

## Job Update

If the state of a single job changes, the server will send an update for that job.  
If the job field is `null`, the job corresponding to the ID has been deleted.

```
SERVER -> WS/JobUpdate(jobId: UInt, job: SimJob?)
```

### Examples

TODO

## Detailed Job Update

The server provides a client with detailed job updates when the client is subscribed to a job.

```
SERVER -> DetailedUpdate(job: SimJob, val conf: String)
```

### Examples

TODO
