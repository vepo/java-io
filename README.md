# Protocolo Ring Hello

Esse protocolo é muito simples. Teremos 4 servidores com implementações diferentes que enviam desafios matemáticos para seus vizinhos. 


```sequence
Alice->Bob: GreetingRequest (message="Hello, I'm Alice!", name="Alice")
Bob-->Alice: GreetingResponse (message="Hi Alice! I'm Bob.", name="Bob")

Bob->Carlos: GreetingRequest (message="Hello, I'm Bob!", name="Bob")
Carlos-->Bob: GreetingResponse (message="Hi, Bob! I'm Carlos.", name="Carlos")

Carlos->Alice: GreetingRequest (message="Hello, I'm Carlos!", name="Carlos")
Alice-->Carlos: GreetingResponse (message="Hi, Carlos. I'm Alice.", name="Alice")

Alice->Bob: DoMath(id=1, number=54, operation='+', history="54")
Bob->Carlos: DoMath(id=1, number=75, operation='-', history="54 + 21")
Carlos->Alice: DoMath(id=1, number=65, operation='*', history="(54 + 21) - 10")
Alice-->Carlos: Done(id=1, result=65)
Carlos-->Bob: Done(id=1, result=65)
Bob-->Alice: Done(id=1, result=65)
```

![Diagrama de Fluxo](https://www.plantuml.com/plantuml/png/VPB1JeD048RlFCM42wnb4p554nD8rIRgmOjlGEt4RhBirDtPWsylWJrq0lP8yiqty__iBd3lEGusUxBc2tNtJ8T7UFM8RDpt9_v430pb6rfB5RmN0qoSoaPEwsxwkEJ3cLp0SS3Cz8YfR8AxbzvR2jVa2Pz1BMzPSS-SojB0yXgnGCoaj8kZn1QHL_Ipfu-UJwKvjdK5BWu7z6sphO3Ew7isvDfYKrHmCe79_xPviysLB67DyD08WvO6sC1TdIiPTbrqBqKtbw8ocHHeg6_pPHDxSfXq7aEqFEhaF_cF4KzY7TcXEyR1_W40)