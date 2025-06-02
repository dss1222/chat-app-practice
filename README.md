![image](https://github.com/user-attachments/assets/bf1c4d88-90dd-48f0-9564-28213a3380aa)
# Chat Application (Server + SDK)

## 프로젝트 개요

본 프로젝트는 **Java 기반 실시간 채팅 어플리케이션**으로,  
**서버-클라이언트 구조**를 명확히 분리하여 설계되었습니다.  

- **Server**: Spring Boot 기반 WebSocket 서버
- **Client SDK**: Java WebSocket 기반 통신 라이브러리
- **확장성**: Redis Pub/Sub 기반 서버 간 메시지 동기화 구조

---

## 시연 화면
![image](https://github.com/user-attachments/assets/0dcd4abc-8903-4927-a009-ad0dedfbceed)
토큰 발급 및 입장 시 토큰 검증
![image](https://github.com/user-attachments/assets/aabce595-9c81-4dd5-97b0-0771db0ba2d2)
채팅방 입장 시 웹소켓 연결 유저 목록 노출
![image](https://github.com/user-attachments/assets/28b59416-8c54-4b5e-9e4c-9b32c9c03353)
연결 해제 시 채팅방 이탈 상태로 갱신


## 아키텍처

```
         [ Client Application (SDK) ]
                   |
                   | (WebSocket + JWT 인증)
                   ▼
         [ Chat Server (Spring Boot) ]
                   |
                   | (메시지 발행/구독)
                   ▼
              [ Redis Pub/Sub ]
                   ▲
                   |
         [ Chat Server (Spring Boot) ]
                   |
                   .
                   .

         [ 멀티 인스턴스 수평 확장 구조 ]
```

- 클라이언트(SDK) → 서버: WebSocket 연결
- 서버 → 클라이언트: 실시간 메시지 브로드캐스트
- 서버 ↔ 서버: Redis Pub/Sub으로 메시지 동기화

---

## 기술 스택

| 기술              | 설명                                       |
|------------------|------------------------------------------|
| Java 17          | 서버 및 SDK 개발 언어                            |
| Spring Boot 3.x  | 서버 프레임워크, WebSocket 지원                  |
| WebSocket        | 클라이언트-서버 간 실시간 양방향 통신               |
| Redis Pub/Sub    | 서버 간 메시지 브로드캐스트                           |
| JWT              | 클라이언트 인증 및 세션 관리                         |
| Gradle           | 프로젝트 빌드 및 의존성 관리                         |
| Java-WebSocket   | 클라이언트 WebSocket 통신 라이브러리                 |

---

## 프로젝트 구조

```plaintext
chat-app-practice/
├── chat-server/        # WebSocket 서버 (Spring Boot)
├── chat-sdk/           # 클라이언트용 Java SDK (라이브러리 형태)
├── sample-app/         # SDK를 사용한 예제 클라이언트
└── README.md
```

---

## 서버 주요 기능 (chat-server)

- **JWT 인증**: WebSocket 연결 요청 시 JWT 토큰 검증
- **WebSocket 통신**: 클라이언트와 양방향 실시간 메시지 송수신
- **Redis Pub/Sub**: 멀티 인스턴스 간 메시지 브로드캐스트
- **유저 목록 관리**: 현재 접속자 목록 실시간 관리 및 전달

---

## 클라이언트 SDK 주요 기능 (chat-sdk)

- **WebSocket 연결**: 서버와의 실시간 통신 기능
- **JWT 토큰 발급 자동화**: 닉네임만 입력하면 SDK가 토큰 발급 요청
- **메시지 송수신 지원**: 채팅 메시지 전송 및 수신 리스너 제공
- **라이브러리 형태 배포**: jar 파일로 빌드 후 외부 프로젝트에 추가 사용 가능

---

## 실행 방법

### 1. 서버 실행

```bash
cd chat-server
./gradlew bootRun
```

> 기본 포트 `localhost:8080`에서 WebSocket 및 API 서비스 제공

---

### 2. Redis 실행

Redis가 설치되어 있어야 합니다.

```bash
redis-server
```

> Redis는 Pub/Sub를 통한 메시지 동기화를 담당합니다.

---

### 3. SDK 빌드

```bash
cd chat-sdk
./gradlew build
```

빌드 후 `build/libs/chat-sdk-0.0.1-SNAPSHOT.jar` 파일 생성

---

### 4. 예제 클라이언트 실행

```bash
cd sample-app
./gradlew run
```

> SDK를 활용하여 서버와 실시간 채팅 테스트 가능

---

## SDK 사용 예시

```java
ChatClient client = new ChatClient(
    "ws://localhost:8080/ws/chat",
    "http://localhost:8080/api/token"
);

client.setNickname("홍길동");

client.setListener(new ChatListener() {
    @Override
    public void onMessage(ChatMessage message) {
        System.out.println(message.getSender() + ": " + message.getMessage());
    }
});

client.connect();
client.sendMessage("안녕하세요, 채팅 테스트입니다!");
```

---

## 향후 확장 고려 사항

- **WebSocket 재연결**: 네트워크 장애 시 자동 재접속 지원
- **WebSocket 연결 수 증가**: 세션 정보를 서버에 접속하지 않고 토큰 인증 상태로 유지
- **JWT 리프레시 토큰**: 토큰 만료 시 자동 재발급
- **그룹 채팅방 지원**: Room 기반 채팅 지원
- **파일 전송 기능**: 텍스트 외 이미지/파일 송수신 지원
- **서버 부하 증가 대응**: 로드밸런서 적용
- **메시지 내용 저장**: DB 도입 및 메시지 기록 저장
---
