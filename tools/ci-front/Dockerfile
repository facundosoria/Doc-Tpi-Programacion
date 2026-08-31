# Front de solo lectura del CI. Sin dependencias: solo biblioteca estandar.
FROM python:3.13-alpine

WORKDIR /app
COPY servidor.py datos-prueba.json ./
COPY static ./static

# Usuario sin privilegios: este proceso queda expuesto en la red y no necesita
# nada de root. Tampoco toca el socket de Docker, a diferencia del runner.
RUN adduser -D -u 10001 front
USER front

EXPOSE 8099
CMD ["python3", "servidor.py"]
