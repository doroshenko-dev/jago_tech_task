## Environment
* make 4.3
* docker 19.03.13
* docker-compose 1.27.4
* python 3.7.6
* sbt 1.5.5
* scala 2.13.6
* java 1.8.0

## Architecture 
Infrastructure consists from:
* 3 Zookeeper containers
* 3 Kafka brokers
* Schema registry container
* Redis container (to persist data from Consumer)
* Producer container (Python application)
* Consumer container (Scala application)

#### Producer
The python application that generates fake transactions as protobuf messages into Kafka topic.

Environment variables (configured at `docker-compose.yml`):
* KAFKA_BROKERS - Comma separated list of Kafka brokers
* SCHEMA_REGISTRY_URL - Schema registry service URL
* TOPIC_NAME - Kafka topic to send messages
* ACCOUNTS_NUM - Number of accounts used to generate transactions

#### Consumer
The scala application that reads protobuf messages from Kafka topic and stores the total value of the transactions made by the account.
The app creates multiple consumers in a consumer group to leverage parallelism of topic partitions.
Use Redis as persistence storage. 

Environment variables (configured at `docker-compose.yml`):
* KAFKA_BROKERS - Comma separated list of Kafka brokers
* SCHEMA_REGISTRY_URL - Schema registry service URL
* TOPIC_NAME - Kafka topic to read messages
* CONSUMERS_NUM - Number of consumers, ideally the same number as the number of topic partitions

## Instructions
#### 1. Go to project root directory 
    cd jago_tech_task 
#### 2. Spin up environment
    make dc-start
#### 3. Wait until a `schema-registry` service will start
    The following log message should appear in `schema-registry` container:
    `INFO Server started, listening for requests... (io.confluent.kafka.schemaregistry.rest.SchemaRegistryMain)`
#### 4. Create `jagoTransactions` topic with 3 partitions
    make topic_name=jagoTransactions partitions=3 create-topic

#### 5. Start consumer
    make run-consumer

#### 6. Open new terminal session and produce 1000 messages
    cd jago_tech_task
    make n=1000 produce-messages

#### 7. Check results
    make read-results

## Helpers
#### To update Protobuf message 
    1. Edit `transactions.proto` file 
    2. Compile message definition classes
        protoc -I=. --java_out=. transactions.proto
        protoc -I=. --python_out=. ./transactions.proto
    3. Copy generated files to appropriate modules

#### Build consumer jar
    cd consumer
    export JAVA_HOME=$(/usr/libexec/java_home -v 1.8.0_292)
    sbt assembly

## Improvements
1. Add unit tests
2. Add integration tests
3. Add CI pipelines to build artifacts
4. Setup services to live on different hosts
5. Add health checks to start Schema Registry container after all Kafka brokers started
6. Setup Kafka without Zookeeper
7. Add deployment to k8s
