class ChatClient {
    constructor() {
        this.socket = null;
        this.nickname = null;
        this.token = null;
        this.initializeEventListeners();
    }

    initializeEventListeners() {
        const nicknameInput = document.getElementById("nickname");
        nicknameInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                this.getToken();
            }
        });

        const messageInput = document.getElementById("message");
        messageInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                this.sendMessage();
            }
        });
    }

    getToken() {
        this.nickname = document.getElementById("nickname").value;
        if (!this.nickname) {
            alert("닉네임을 입력하세요!");
            return;
        }

        fetch("/api/token?username=" + encodeURIComponent(this.nickname))
            .then(response => response.text())
            .then(data => {
                this.token = data;
                localStorage.setItem("access_token", this.token);
                alert("토큰 발급 완료!");
            })
            .catch(error => {
                console.error("토큰 발급 실패", error);
            });
    }

    connect() {
        this.token = localStorage.getItem("access_token");

        if (!this.token) {
            alert("토큰이 없습니다! 먼저 토큰을 발급받아주세요!");
            return;
        }

        this.socket = new WebSocket("ws://" + window.location.host + "/ws/chat");

        this.socket.onopen = () => {
            const enterMessage = {
                type: "ENTER",
                sender: this.nickname,
                message: this.token
            };
            this.socket.send(JSON.stringify(enterMessage));
        };

        this.socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.type === "USER_LIST") {
                    this.updateUserList(data.users);
                } else {
                    this.appendMessage(event.data);
                }
            } catch {
                this.appendMessage(event.data);
            }
        };

        this.socket.onclose = () => {
            this.appendMessage("🔕 연결이 종료되었습니다.");
        };
    }

    sendMessage() {
        const messageBox = document.getElementById("message");
        const content = messageBox.value;
        if (this.socket && this.socket.readyState === WebSocket.OPEN && content.trim() !== "") {
            const chatMessage = {
                type: "CHAT",
                sender: this.nickname,
                message: content
            };
            this.socket.send(JSON.stringify(chatMessage));
            messageBox.value = "";
        }
    }

    appendMessage(message) {
        const chatBox = document.getElementById("chat-box");
        chatBox.innerHTML += message + "<br>";
        chatBox.scrollTop = chatBox.scrollHeight;
    }

    updateUserList(users) {
        const list = document.getElementById("user-list");
        list.innerHTML = "";
        users.forEach(user => {
            const li = document.createElement("li");
            li.textContent = user;
            list.appendChild(li);
        });
    }
}

// Initialize chat client when DOM is loaded
document.addEventListener("DOMContentLoaded", () => {
    window.chatClient = new ChatClient();
}); 