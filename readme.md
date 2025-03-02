# RAG using  Spring AI + Postgres Vector DB

## Pre-requisite

- Docker installed
- Ollama installed

## SETUP

### Postgres DB

```shell
mkdir ~/postgres-volume
```

- Using docker-compose to run Postgres DB
```shell
docker compose -f docker-compose.yaml up
```
![img.png](img.png)

- add vector extension and create vector-store table
![img_1.png](img_1.png)



## References

- https://github.com/YugabyteDB-Samples/openai-pgvector-lodging-service
- https://www.youtube.com/watch?v=ctsGQ3lhcYA
