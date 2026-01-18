FROM python:3.13-slim
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY gtfs_processor.py .
COPY import_transport_to_neo4j.py .

RUN mkdir gtfs_data

CMD ["sh", "-c", "python gtfs_processor.py && python import_transport_to_neo4j.py"]