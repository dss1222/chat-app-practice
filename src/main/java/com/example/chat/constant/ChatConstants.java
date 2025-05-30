package com.example.chat.constant;

public final class ChatConstants {
    private ChatConstants() {
    } // 인스턴스화 방지

    // 메시지 포맷
    public static final String ENTER_MESSAGE_FORMAT = "🔵 [%s] 님이 입장하셨습니다.";
    public static final String LEAVE_MESSAGE_FORMAT = "🔴 [%s] 님이 퇴장하셨습니다.";
    public static final String CHAT_MESSAGE_FORMAT = "💬 [%s]: %s";
    public static final String DISCONNECT_MESSAGE_FORMAT = "🔴 [%s] 님의 연결이 종료되었습니다.";
    public static final String UNKNOWN_MESSAGE = "[알 수 없는 메시지]";

    // 메시지 타입
    public static final String USER_LIST_TYPE = "USER_LIST";
}