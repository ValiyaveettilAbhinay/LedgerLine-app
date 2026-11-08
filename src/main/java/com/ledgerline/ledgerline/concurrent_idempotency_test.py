import json
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8080/api/transactions/transfer"

JWT_TOKEN = "9FUdx+I0+4ZEsCZAqaqJ/6upw8zSU6w0PlecqP8nug="

IDEMPOTENCY_KEY = "concurrent-transfer-001"

payload = {
    "sourceAccountId": "YOUR_SOURCE_ACCOUNT_UUID",
    "destinationAccountNumber": "YOUR_DESTINATION_ACCOUNT_NUMBER",
    "amountMinorUnits": 50000,
    "description": "Concurrent idempotency test"
}


def send_request():

    data = json.dumps(payload).encode("utf-8")

    request = urllib.request.Request(
        URL,
        data=data,
        headers={
            "Authorization": f"Bearer {JWT_TOKEN}",
            "Content-Type": "application/json",
            "Idempotency-Key": IDEMPOTENCY_KEY
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(request) as response:
            return response.status, response.read().decode()

    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()


with ThreadPoolExecutor(max_workers=2) as executor:

    futures = [
        executor.submit(send_request),
        executor.submit(send_request)
    ]

    results = [future.result() for future in futures]


for index, result in enumerate(results, start=1):

    print(f"\nRequest {index}")
    print("HTTP Status:", result[0])
    print("Response:", result[1])