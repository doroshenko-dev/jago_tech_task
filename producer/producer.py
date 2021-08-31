#!/usr/bin/env python3

import argparse
import os
import random
import logging
from sys import stdout
from uuid import uuid4

import transactions_pb2
from confluent_kafka import SerializingProducer
from confluent_kafka.serialization import StringSerializer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.protobuf import ProtobufSerializer
from faker import Faker
from faker.providers import bank

logger = logging.getLogger(__name__)
handler = logging.StreamHandler(stdout)
formatter = logging.Formatter("%(asctime)s %(levelname)s %(filename)s:%(funcName)s %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)
logger.setLevel(logging.DEBUG)

topic_name = os.getenv("TOPIC_NAME")
schema_registry_url = os.getenv("SCHEMA_REGISTRY_URL")
kafka_brokers = os.getenv("KAFKA_BROKERS")
accounts_num = int(os.getenv("ACCOUNTS_NUM"))


def setup_producer():
    schema_registry_client = SchemaRegistryClient({'url': schema_registry_url})
    protobuf_serializer = ProtobufSerializer(transactions_pb2.Transaction, schema_registry_client)
    producer_conf = {'bootstrap.servers': kafka_brokers,
                     'key.serializer': StringSerializer('utf_8'),
                     'value.serializer': protobuf_serializer}
    logger.info(f"Producer config - {producer_conf}")
    producer = SerializingProducer(producer_conf)
    return producer


def main(args):
    producer = setup_producer()
    fake = Faker()
    fake.add_provider(bank)

    accounts = [fake.iban() for _ in range(accounts_num)]
    logger.info(f"Generate transactions for - {accounts}")
    logger.info(f"Generating {args.num} transactions into {topic_name} Kafka topic")
    for i in range(args.num + 1):
        producer.poll(0.0)
        transaction = transactions_pb2.Transaction()
        transaction.transaction_id = str(uuid4())
        transaction.account_number = accounts[random.randint(0, accounts_num - 1)]
        transaction.transaction_reference = fake.swift8()
        transaction.transaction_datetime.GetCurrentTime()
        transaction.amount = round(random.uniform(1.0, 1000.0), 2)

        producer.produce(topic=topic_name, key=transaction.account_number, value=transaction)
        if i % 10 == 0:
            logger.info(f"Generated {i} messages")
    producer.flush()
    logger.info("Message generation completed")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Jago transactions producer")
    parser.add_argument('-n', dest="num", type=int, default=1000, help="number of transactions to generate")

    main(parser.parse_args())
