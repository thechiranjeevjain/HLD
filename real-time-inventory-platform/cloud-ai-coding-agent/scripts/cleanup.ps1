docker ps -aq --filter 'name=agent-' | ForEach-Object { docker rm -f $_ }
