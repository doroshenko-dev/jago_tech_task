name := "consumer"
version := "0.1"
scalaVersion := "2.13.6"
resolvers += "confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "2.8.0",
  "io.confluent" % "kafka-protobuf-serializer" % "6.2.0",
  "net.debasishg" %% "redisclient" % "3.30",
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}