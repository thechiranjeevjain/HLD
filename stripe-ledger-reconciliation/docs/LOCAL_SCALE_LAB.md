# Local Scale and Multi-instance Lab

Generate a deterministic dataset after resetting the demo:

```powershell
$body = @{ transactions = 100000; currency = 'USD'; mismatchPercent = 2 } | ConvertTo-Json
Invoke-RestMethod http://localhost:8095/api/demo/generate-scale -Method Post -ContentType application/json -Body $body
```

The API caps a single request at one million payments, writes two balanced ledger lines per payment, and creates a chosen mismatch percentage. Start with 10,000, observe memory and disk, then increase. Record machine specifications and elapsed time before quoting results.

To demonstrate region-like request races, run two application processes on different ports against the same PostgreSQL database and send the same payment key to both. The integration suite already launches 16 concurrent calls through the transactional service and proves one payment/journal owner.

This demonstrates the correctness protocol, not geographic latency or a real million-events-per-day SLA. Those require representative infrastructure and sustained load tests.
