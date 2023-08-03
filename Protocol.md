# Protocol

This document describes the communication protocol between clients and the server.  
All messages must be encoded in JSON format.  
The examples use `curl` and [`websocat`](https://github.com/vi/websocat) to communicate with the server.  
To parse JSON responses, `jq` is used.

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

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Authenticate WebSocket session
response=$(echo "{\"type\":\"WebSocketAuth\",\"bearerToken\":\"$bearer_token\"}" | \
  websocat 'ws://0.0.0.0:8080/')

echo "$response"

```

The example above should produce the following output:

```json
{
  "type": "WebSocketAuthResponse",
  "success": true
}
```

## Get Options Available for Manual Stage Configuration

The server provides the available options for manual configuration.

```
CLIENT -> GET/options/available
Server -> Option
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get available options
response=$(curl -s 'http://0.0.0.0:8080/options/available' \
  -H "Authorization: $bearer_token")

echo "$response"

```

The example above should produce a very long JSON response.

## Get Properties Available for Properties Stage Configuration

The server provides the available properties for manual configuration.

```
CLIENT -> GET/options/available/properties
Server -> Option
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get available options
response=$(curl -s 'http://0.0.0.0:8080/options/available/properties' \
  -H "Authorization: $bearer_token")

echo "$response"

```

The example above should produce a long JSON response.

## Get Default Manual Stage Configurations

The server also provides some default stages that represent a good starting point for relaxation and simulation.

```
CLIENT -> GET/options/default
SERVER -> List<StageConfig>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get default options
response=$(curl -s 'http://0.0.0.0:8080/options/default' \
  -H "Authorization: $bearer_token")

echo "$response"

```

The example above should produce a very long JSON response.

## Get Default File Stage Configurations

The server also provides some default stages that represent a good starting point for relaxation and simulation.

```
CLIENT -> GET/options/default/files
SERVER -> List<StageConfig>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get default options
response=$(curl -s 'http://0.0.0.0:8080/options/default/files' \
  -H "Authorization: $bearer_token")

echo "$response"

```

The example above should produce a very long JSON response.

## Get Default Properties Stage Configurations

The server also provides some default stages that represent a good starting point for relaxation and simulation.

```
CLIENT -> GET/options/default/properties
SERVER -> List<StageConfig>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get default options
response=$(curl -s 'http://0.0.0.0:8080/options/default/properties' \
  -H "Authorization: $bearer_token")

echo "$response"

```

The example above should produce a long JSON response.

## Get Job list

A client can get the list of all jobs via a simple GET request.

```
CLIENT -> GET/job
SERVER -> List<SimJob>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get jobs
response=$(curl -s 'http://0.0.0.0:8080/job' \
  -H "Authorization: $bearer_token")

echo "$response"

```

The example above should produce a JSON list of jobs, e.g.:

```json
[
  {
    "metadata": {
      "title": "Some Job",
      "description": "A very important Job"
    },
    "id": 0,
    "stages": 4,
    "completedStages": 4,
    "status": "DONE",
    "progress": 1.0,
    "extensions": 200,
    "error": null
  },
  {
    "metadata": {
      "title": "Some Job",
      "description": "A very important Job"
    },
    "id": 1,
    "stages": 4,
    "completedStages": 3,
    "status": "RUNNING",
    "progress": 0.0030904198,
    "extensions": 200,
    "error": null
  },
  {
    "metadata": {
      "title": "Some Job",
      "description": "A very important Job"
    },
    "id": 2,
    "stages": 4,
    "completedStages": 0,
    "status": "NEW",
    "progress": 0.0,
    "extensions": 0,
    "error": null
  }
]
```

## Get Job

A client can retrieve a single job by its ID.

```
CLIENT -> GET/job/<ID>
SERVER -> SimJob
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get job with ID 0
response=$(curl -s 'http://0.0.0.0:8080/job/0' \
  -H "Authorization: $bearer_token")

echo "$response"

```

Depending on whether there is currently a job with ID 0, the response should be a job or 404.
If a job with ID 0 exists:

```json
{
  "metadata": {
    "title": "Some Job",
    "description": "A very important Job"
  },
  "id": 0,
  "stages": 4,
  "completedStages": 4,
  "status": "DONE",
  "progress": 1.0,
  "extensions": 200,
  "error": null
}
```

## Get Job Details

A client can retrieve a single job along with its top dat/conf and forces data by its ID.  
The dat data is taken from the most recently completed stage.

```
CLIENT -> GET/job/details/<ID>
SERVER -> CompleteJob(job: SimJob, top: String, dat: String, forces: String)
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get details of job with ID 0
response=$(curl -s 'http://0.0.0.0:8080/job/details/0' \
  -H "Authorization: $bearer_token")

echo "$response"

```

Depending on whether there is currently a job with ID 0, the response should be a job and its data files or 404.
If a job with ID 0 exists:

```json
{
  "job": {
    "metadata": {
      "title": "Some Job",
      "description": "A very important Job"
    },
    "id": 0,
    "stages": 4,
    "completedStages": 3,
    "status": "RUNNING",
    "progress": 0.67101985,
    "extensions": 200,
    "error": null
  },
  "top": "top data",
  "dat": "dat data",
  "forces": "forces data"
}
```

## Download Job

A client can retrieve all data stored on the server as a zip file.

```
CLIENT -> GET/job/download/<ID>
SERVER -> download.zip
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get all files of job with ID 0 as zip file
status_code=$(curl -s -o /dev/null -w "%{http_code}" 'http://0.0.0.0:8080/job/download/0' \
  -H "Authorization: $bearer_token")

echo "$status_code"

```

Depending on whether there is currently a job with ID 0, the response should be a zip file or 404.

## New Job

A new job can be submitted using a POST request.

```
CLIENT -> POST/job: JobNew(metadata: Map<String, String>, configs: List<StageConfig>, top: String, dat: String, forces: String)
SERVER -> SimJob
```

### Examples

Submission of a new job with the default configurations:

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get the default stages
defaults=$(curl -s 'http://0.0.0.0:8080/options/default' \
  -H "Authorization: $bearer_token")

# Read test data files
top=$(cat './src/test/resources/tetrahedron.top')
dat=$(cat './src/test/resources/tetrahedron.dat')
forces=$(cat './src/test/resources/tetrahedron.forces')

# Save the JSON data to a temporary file
# Otherwise the command would be too long
tmp_file=$(mktemp)
echo "{\"metadata\":{\"title\":\"Some Job\",\"description\":\"A very important Job\"},\"configs\":$defaults,\"top\":\"$top\",\"dat\":\"$dat\",\"forces\":\"$forces\"}" >"$tmp_file"

# Submit new job
# Use --data-binary to read the JSON data from the temporary file
response=$(curl -s 'http://0.0.0.0:8080/job' \
  -H "Authorization: $bearer_token" \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "@$tmp_file")

# Clean up the temporary file
rm "$tmp_file"

echo "$response"

```

Submission of a new job with the default file configurations:

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get the default stages
defaults=$(curl -s 'http://0.0.0.0:8080/options/default/files' \
  -H "Authorization: $bearer_token")

# Read test data files
top=$(cat './src/test/resources/tetrahedron.top')
dat=$(cat './src/test/resources/tetrahedron.dat')
forces=$(cat './src/test/resources/tetrahedron.forces')

# Save the JSON data to a temporary file
# Otherwise the command would be too long
tmp_file=$(mktemp)
echo "{\"metadata\":{\"title\":\"Some Job\",\"description\":\"A very important Job\"},\"configs\":$defaults,\"top\":\"$top\",\"dat\":\"$dat\",\"forces\":\"$forces\"}" >"$tmp_file"

# Submit new job
# Use --data-binary to read the JSON data from the temporary file
response=$(curl -s 'http://0.0.0.0:8080/job' \
  -H "Authorization: $bearer_token" \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "@$tmp_file")

# Clean up the temporary file
rm "$tmp_file"

echo "$response"

```

Submission of a new job with the default flat property configurations:

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get the default stages
defaults=$(curl -s 'http://0.0.0.0:8080/options/default/properties' \
  -H "Authorization: $bearer_token")

# Read test data files
top=$(cat './src/test/resources/tetrahedron.top')
dat=$(cat './src/test/resources/tetrahedron.dat')
forces=$(cat './src/test/resources/tetrahedron.forces')

# Save the JSON data to a temporary file
# Otherwise the command would be too long
tmp_file=$(mktemp)
echo "{\"metadata\":{\"title\":\"Some Job\",\"description\":\"A very important Job\"},\"configs\":$defaults,\"top\":\"$top\",\"dat\":\"$dat\",\"forces\":\"$forces\"}" >"$tmp_file"

# Submit new job
# Use --data-binary to read the JSON data from the temporary file
response=$(curl -s 'http://0.0.0.0:8080/job' \
  -H "Authorization: $bearer_token" \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "@$tmp_file")

# Clean up the temporary file
rm "$tmp_file"

echo "$response"

```

The example above should produce a new job, e.g.:

```json
{
  "metadata": {
    "title": "Some Job",
    "description": "A very important Job"
  },
  "id": 1,
  "stages": 4,
  "completedStages": 0,
  "status": "NEW",
  "progress": 0.0,
  "extensions": 0,
  "error": null
}
```

## Delete Job

A job can be deleted by its ID using a DELETE request.

```
CLIENT -> DELETE/job/<ID>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Delete the job with ID 0
status_code=$(curl -s -o /dev/null -w "%{http_code}" 'http://0.0.0.0:8080/job/0' \
  -H "Authorization: $bearer_token" \
  -X DELETE)

echo "$status_code"

```

Depending on whether there is currently a job with ID 0, the response should be 200 or 404.

## Cancel Job

A job can be canceled by its ID using a PATCH request.

```
CLIENT -> PATCH/job/<ID>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Cancel the job with ID 0
status_code=$(curl -s -o /dev/null -w "%{http_code}" 'http://0.0.0.0:8080/job/0' \
  -H "Authorization: $bearer_token" \
  -X PATCH)

echo "$status_code"

```

Depending on whether there is currently a job with ID 0, the response should be 200 or 404.

## Get Subscription

The current subscription can be checked with a GET request.  
If the client isn't currently subscribed to a job, the server will respond with `204 No Content`.  
Otherwise, the response will contain the job ID.

```
CLIENT -> GET/job/subscribe
SERVER -> UInt
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get subscription
response=$(curl -s 'http://0.0.0.0:8080/job/subscribe' \
  -H "Authorization: $bearer_token")

echo "$response"

```

Depending on whether there is currently an active subscription, the response should be a job ID or 204.

## Subscribe Job

If a client wants to receive detailed updates for a job, they can subscribe with a POST request.  
Normal job updates will still be delivered.

```
CLIENT -> POST/job/subscribe/<ID>
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Subscribe to job with ID 0
status_code=$(curl -s -o /dev/null -w "%{http_code}" 'http://0.0.0.0:8080/job/subscribe/0' \
  -H "Authorization: $bearer_token" \
  -X POST)

echo "$status_code"

```

Depending on whether there is currently a job with ID 0, the response should be 200 or 404.

## Unsubscribe

If a client doesn't want to receive detailed updates anymore, they can unsubscribe with a DELETE request.  
Normal job updates will still be delivered.

```
CLIENT -> DELETE/job/subscribe
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Unsubscribe
status_code=$(curl -s -o /dev/null -w "%{http_code}" 'http://0.0.0.0:8080/job/subscribe' \
  -H "Authorization: $bearer_token" \
  -X DELETE)

echo "$status_code"

```

The response should always be 200.

## Job Update

If the state of a single job changes, the server will send an update for that job.  
If the job field is `null`, the job corresponding to the ID has been deleted.

```
SERVER -> WS/JobUpdate(jobId: UInt, job: SimJob?)
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get the default stages
defaults=$(curl -s 'http://0.0.0.0:8080/options/default' \
  -H "Authorization: $bearer_token")

# Read test data files
top=$(cat './src/test/resources/tetrahedron.top')
dat=$(cat './src/test/resources/tetrahedron.dat')
forces=$(cat './src/test/resources/tetrahedron.forces')

# Save the JSON data to a temporary file
# Otherwise the command would be too long
tmp_file=$(mktemp)
echo "{\"metadata\":{\"title\":\"Some Job\",\"description\":\"A very important Job\"},\"configs\":$defaults,\"top\":\"$top\",\"dat\":\"$dat\",\"forces\":\"$forces\"}" >"$tmp_file"

# Submit new job
# Use --data-binary to read the JSON data from the temporary file
curl -s 'http://0.0.0.0:8080/job' \
  -H "Authorization: $bearer_token" \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "@$tmp_file"

# Clean up the temporary file
rm "$tmp_file"

# Authenticate WebSocket session and listen to updates
echo "{\"type\":\"WebSocketAuth\",\"bearerToken\":\"$bearer_token\"}" | \
  websocat -n 'ws://0.0.0.0:8080/'

```

Over time, updates such as the following should be received:

```json
{
  "type": "JobUpdate",
  "jobId": 0,
  "job": {
    "metadata": {
      "title": "Some Job",
      "description": "A very important Job"
    },
    "id": 0,
    "stages": 4,
    "completedStages": 2,
    "status": "RUNNING",
    "progress": 0.0029907287,
    "extensions": 95,
    "error": null
  }
}
```

## Detailed Job Update

The server provides a client with detailed job updates when the client is subscribed to a job.

```
SERVER -> DetailedUpdate(job: SimJob, val top: String, val dat: String)
```

### Example

```shell
#!/bin/bash

# Get a bearer token from the auth endpoint
bearer_token=$(curl -s 'http://0.0.0.0:8080/auth' \
  -H 'Authorization: ChangeMe')

# Get the default stages
defaults=$(curl -s 'http://0.0.0.0:8080/options/default' \
  -H "Authorization: $bearer_token")

# Read test data files
top=$(cat './src/test/resources/tetrahedron.top')
dat=$(cat './src/test/resources/tetrahedron.dat')
forces=$(cat './src/test/resources/tetrahedron.forces')

# Save the JSON data to a temporary file
# Otherwise the command would be too long
tmp_file=$(mktemp)
echo "{\"metadata\":{\"title\":\"Some Job\",\"description\":\"A very important Job\"},\"configs\":$defaults,\"top\":\"$top\",\"dat\":\"$dat\",\"forces\":\"$forces\"}" >"$tmp_file"

# Submit new job
# Use --data-binary to read the JSON data from the temporary file
job=$(curl -s 'http://0.0.0.0:8080/job' \
  -H "Authorization: $bearer_token" \
  -X POST \
  -H "Content-Type: application/json" \
  --data-binary "@$tmp_file")

job_id=$(echo "$job" | jq -r '.id')

# Clean up the temporary file
rm "$tmp_file"

# Subscribe to the newly created job
curl -s -o /dev/null -w "%{http_code}" "http://0.0.0.0:8080/job/subscribe/$job_id" \
  -H "Authorization: $bearer_token" \
  -X POST

# Authenticate WebSocket session and listen to updates
echo "{\"type\":\"WebSocketAuth\",\"bearerToken\":\"$bearer_token\"}" | \
  websocat -n 'ws://0.0.0.0:8080/'

```

Over time, detailed updates should be received.
