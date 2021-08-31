FROM python:3.7.6-slim-buster
ENV PYTHONUNBUFFERED=1

WORKDIR /app
COPY producer/ /app/
RUN pip install --no-cache-dir -r /app/requirements.txt
RUN useradd -N -G users -u 1313 app && chown app:users -R .
USER app
RUN chmod +x /app/producer.py

CMD ["tail", "-f", "/dev/null"]