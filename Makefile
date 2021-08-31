dc-start:
	docker-compose up -V -d --build --force-recreate

create-topic:
	docker exec kafka-1 kafka-topics \
      --create \
      --bootstrap-server localhost:19092 \
      --replication-factor 1 \
      --partitions $(partitions) \
      --topic $(topic_name)

run-consumer:
	docker exec consumer java -jar /app/consumer-assembly-0.1.jar

produce-messages:
	docker exec producer /app/producer.py -n $(n)

read-results:
	for key in `docker exec redis redis-cli keys "*"`; do 						\
  		echo "Account $$key has total transactions amount -> `docker exec redis redis-cli GET $$key`"; 	\
	done
