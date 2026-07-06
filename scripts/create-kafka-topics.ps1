$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$topics = @("logs", "metrics", "alerts", "incidents", "dlq")

Push-Location $projectRoot
try {
    foreach ($topic in $topics) {
        docker compose exec -T kafka-1 kafka-topics `
            --bootstrap-server kafka-1:29092 `
            --create `
            --if-not-exists `
            --topic $topic `
            --partitions 3 `
            --replication-factor 3
    }

    docker compose exec -T kafka-1 kafka-topics `
        --bootstrap-server kafka-1:29092 `
        --list
}
finally {
    Pop-Location
}
