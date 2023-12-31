# S3
This readme is about the Springboot backend using MinIO and PostgreSQL.

The following stack is used:
- `springframework.boot:3.0.5`
- `minio:latest`
- `postgres:latest`

## Environments

| Environment  | Website                                      |
| ------------ | -------------------------------------------- |
| `development` | See issue #25 |
| `production` | See issue #25 |

## Tooling
- [GitLab Issue Boards](https://gitlab.inf-hsleiden.nl/2223.ipsenh-p3/s3/-/boards/103)

## Getting started

### Prerequisites
- [OpenJDK 17](https://openjdk.org/projects/jdk/17/)
- [Docker](https://www.docker.com/)

### Installation (production)
- Coming soon

### Installation (development)
1. Run `docker compose up`
2. Create access key in [MinIO Console](http://localhost:9090/access-keys)
3. Setup `application.properties`, see `setup.properties`
4. Paste `secretKey` and `accessKey` inside  `application.properties`
5. Install `pom.xml` dependencies
6. Run main class inside `ObjectStorageApplication`
