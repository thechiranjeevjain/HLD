FROM python:3.13-alpine
RUN adduser -D -u 10001 nff
USER nff
COPY sidecar.py /sidecar.py
EXPOSE 8091
CMD ["python","/sidecar.py"]

