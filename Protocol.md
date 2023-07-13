# Protocol

This document describes the communication protocol between client and server.  
All messages are in json format and should be sent over the WebSocket.

## Authentication

If authentication is not enabled (`ACCESSTOKEN` environment variable is blank), the server sends an authentication
success message upon connection.

```
SERVER -> authenticated -> CLIENT
```

If authentication is enabled (non-blank `ACCESSTOKEN` environment variable), a client must first send an
authentication request.

```
CLIENT -> authenticate(accesstoken) -> SERVER
SERVER -> un-/authenticated -> CLIENT
```

### Examples

TODO

## Jobs list update

Upon successful authentication, the server provides the client with a list of available jobs and their status.

```
SERVER -> jobs(list(job)) -> CLIENT
```

### Examples

TODO

## Single Job Update

If the state of a single job changes, the server will not send a list of all jobs, but only an update for that single
job.

```
SERVER -> update(job_id, job?) -> CLIENT
```

### Examples

TODO

## New Job

When the client submits a new job, the server responds with a single job update (see above).

```
CLIENT -> new(job) -> SERVER
```

### Examples

TODO

## Cancel Job

When the client cancels a job, the server responds with a single job update (see above).

```
CLIENT -> cancel(job_id) -> SERVER
```

### Examples

TODO

## Delete Job

When the client deletes a job, the server responds with a single job update (see above).

```
CLIENT -> delete(job_id) -> SERVER
```

### Examples

TODO

## Subscribe Job

If the customer wants to receive detailed job and structure updates, they can subscribe to a job.

```
CLIENT -> subscribe(job_id?) -> SERVER
```

### Examples

TODO

## Detailed Update

The server provides a client with detailed job updates when the client is subscribed to a job.

```
SERVER -> detailed_update(job_id) -> CLIENT
```

### Examples

TODO
