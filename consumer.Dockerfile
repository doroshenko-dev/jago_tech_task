FROM java:8
WORKDIR /app
COPY consumer/target/scala-2.13/consumer-assembly-0.1.jar /app/
RUN useradd -N -G users -u 1313 app && chown app:users -R .
USER app
CMD ["tail", "-f", "/dev/null"]