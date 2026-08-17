# Web Server Lab Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\web-server-lab
mvn clean package
java -jar target/web-server-lab-0.1.0-SNAPSHOT.jar
```

## Walkthrough

1. Fast route.

```powershell
Invoke-WebRequest http://localhost:8080/
```

2. Slow route.

```powershell
Measure-Command { Invoke-WebRequest http://localhost:8080/slow }
```

3. Missing route.

```powershell
Invoke-WebRequest http://localhost:8080/missing
```

## Interview Close

Say: the goal is not feature completeness. The goal is to show blocking, bounded resources, slow-client risk, and overload behavior clearly.
