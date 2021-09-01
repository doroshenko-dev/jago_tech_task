package main.scala

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}

import java.time.Duration
import java.util.{Collections, Properties}
import scala.jdk.CollectionConverters._
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig
import com.redis.RedisClient
import main.scala.Transactions.Transaction

import java.util.concurrent.Executors


object Consumer extends App {
  val props = new Properties()
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, sys.env("KAFKA_BROKERS"))
  props.put(ConsumerConfig.GROUP_ID_CONFIG, sys.env("CONSUMER_GROUP"))
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer")
  props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, classOf[Transaction].getName)
  props.put("schema.registry.url", sys.env("SCHEMA_REGISTRY_URL"))
  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val consumersNum = sys.env("CONSUMERS_NUM").toInt
  val executorService = Executors.newFixedThreadPool(consumersNum)
  for (consumerId <- 0 until consumersNum) {
    executorService.execute(() => startConsumer(consumerId))
  }

  def startConsumer(consumerId: Int): Unit = {
    val redis = new RedisClient()
    val consumer = new KafkaConsumer[String, Transaction](props)
    val topic = sys.env("TOPIC_NAME")
    consumer.subscribe(Collections.singletonList(topic))
    try {
      while (true) {
        val records = consumer.poll(Duration.ofSeconds(1))
        for (record <- records.asScala) {
          System.out.printf("consumer=%d, partition=%d, offset = %d, key = %s, amount = %s \n",
            consumerId, record.partition(), record.offset, record.key, record.value.getAmount)
          val currentAmount = redis.get(record.key).getOrElse(0.0).toString.toDouble
          val newAmount = currentAmount + record.value.getAmount
          redis.set(record.value.getAccountNumber, newAmount)
          System.out.printf("Account %s has total transactions amount -> %s \n", record.key, newAmount)
        }
      }
    }
    finally consumer.close()
  }
}