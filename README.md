# interlok-jclouds

[![license](https://img.shields.io/github/license/interlok-testing/testing_jclouds.svg)](https://github.com/interlok-testing/testing_jclouds/blob/develop/LICENSE)
[![Actions Status](https://github.com/interlok-testing/testing_jclouds/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/interlok-testing/testing_jclouds/actions/workflows/gradle-build.yml)

Project tests interlok-jclouds features

## What it does

Interlok configuration that has three workflows that implement a simple api to manage AWS S3 operations via Jclouds:

* Workflow that exposes `/api/s3/..` REST style operations
* Workflow that exposes `/api/s3-sts/..` REST style operations (Doing the same as `/api/s3/..` but with STS credentials)
* Workflow that exposes `/api/s3utils/...` for everything not covered in s3 REST workflow

Inspired by this Interlok interop project: [Testing Interlok interoperability with AWS](https://interlok.adaptris.net/blog/2019/08/30/interlok-interop-with-aws.html)

## Getting started

We assume that you have an AWS S3 account.
Add a new _variables-local.properties_ file in _/src/main/interlok/config_ with:

```properties
accessKey=Your AWS Access Key
secretKey=Your AWS Secret Key
stsSessionToken=Your STS Session Token
bucket=Your AWS S3 Bucket
```

Then start Interlok

* `./gradlew clean build`
* `(cd ./build/distribution && java -jar lib/interlok-boot.jar)`

## Testing

### S3

```shell
# Upload File
curl -X POST -H "Content-Type: application/json" -d '{"key": "value"}' "http://localhost:8080/api/s3/file.txt"
# List Files
curl "http://localhost:8080/api/s3/"
# Get File (download)
curl "http://localhost:8080/api/s3/file.txt"
# Delete File
curl -X DELETE "http://localhost:8080/api/s3/file.txt"
# Copy File
curl "http://localhost:8080/api/s3utils/copy?from=file.txt&to=other.txt"
# Check File Exists
curl "http://localhost:8080/api/s3utils/check-file-exists?key=nothere.txt"
```

### S3 With STS

```shell
# Upload File
curl -X POST -H "Content-Type: application/json" -d '{"key": "value"}' "http://localhost:8080/api/s3-sts/file.txt"
# List Files
curl "http://localhost:8080/api/s3-sts/"
# Get File (download)
curl "http://localhost:8080/api/s3-sts/file.txt"
# Delete File
curl -X DELETE "http://localhost:8080/api/s3-sts/file.txt"
```
