package com.aenigma.socket.discord.service;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.service.ChatService;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.repository.UserRepository;
import com.aenigma.socket.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
// Note: Use fully qualified name net.dv8tion.jda.api.entities.User to avoid conflict with domain User
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discord ↔ 웹 채팅 동기화 서비스
 * 
 * Discord에서 온 메시지를 웹으로, 웹에서 온 메시지를 Discord로 전송합니다.
 */
@Service
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DiscordChatSyncService extends ListenerAdapter {

    private final DiscordBotService discordBotService;
    private final ChatService chatService;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Discord 채널 ID -> 게임 ID 매핑
    private final Map<String, UUID> channelGameMap = new ConcurrentHashMap<>();
    // Discord 유저 ID -> 시스템 User ID 매핑
    private final Map<String, UUID> discordUserMap = new ConcurrentHashMap<>();
    // 게임 ID -> Discord 채널 ID 매핑 (역방향)
    private final Map<UUID, String> gameChannelMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (discordBotService.isConnected()) {
            discordBotService.getJda().addEventListener(this);
            log.info("Discord 채팅 동기화 서비스 초기화 완료");
        }
    }

    /**
     * Discord 채널과 게임을 바인딩
     */
    public void bindChannelToGame(String channelId, UUID gameId) {
        channelGameMap.put(channelId, gameId);
        gameChannelMap.put(gameId, channelId);
        log.info("Discord 채널 바인딩: channelId={}, gameId={}", channelId, gameId);
    }

    /**
     * Discord 유저와 시스템 유저 매핑
     */
    public void linkDiscordUser(String discordUserId, UUID userId) {
        discordUserMap.put(discordUserId, userId);
        log.info("Discord 유저 연결: discordId={}, userId={}", discordUserId, userId);
    }

    /**
     * Discord 메시지 수신 -> 웹으로 전송
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 봇 메시지 무시
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentDisplay();
        net.dv8tion.jda.api.entities.User author = event.getAuthor();

        // DM 메시지 처리 (밀담)
        if (event.isFromType(ChannelType.PRIVATE)) {
            handlePrivateMessage(author, content, message);
            return;
        }

        // 텍스트 채널 메시지 처리 (공개 채팅)
        if (event.isFromType(ChannelType.TEXT)) {
            handleChannelMessage(event.getChannel().asTextChannel(), author, content);
        }
    }

    /**
     * Discord 공개 채널 메시지 -> 웹 공개 채팅
     */
    private void handleChannelMessage(TextChannel channel, net.dv8tion.jda.api.entities.User author, String content) {
        String channelId = channel.getId();
        UUID gameId = channelGameMap.get(channelId);

        if (gameId == null) {
            log.debug("바인딩되지 않은 채널의 메시지: channelId={}", channelId);
            return;
        }

        UUID userId = discordUserMap.get(author.getId());
        if (userId == null) {
            log.debug("연결되지 않은 Discord 유저: discordId={}", author.getId());
            // Discord 닉네임으로 시스템 메시지 전송
            sendSystemMessageToWeb(gameId, "[Discord] " + author.getName() + ": " + content);
            return;
        }

        // 메시지 저장 및 웹으로 브로드캐스트
        try {
            ChatMessage chatMessage = chatService.sendPublicMessage(gameId, userId, content);

            // WebSocket으로 전송
            ChatResponse response = buildChatResponse(chatMessage, author.getName());
            messagingTemplate.convertAndSend("/topic/game/" + gameId, response);

            log.debug("Discord -> 웹 공개 채팅: gameId={}, user={}", gameId, author.getName());
        } catch (Exception e) {
            log.error("Discord 메시지 처리 실패", e);
        }
    }

    /**
     * Discord DM 메시지 -> 웹 밀담
     * 
     * DM 형식: "@닉네임 메시지내용" 또는 그냥 메시지
     */
    private void handlePrivateMessage(net.dv8tion.jda.api.entities.User author, String content, Message message) {
        UUID senderId = discordUserMap.get(author.getId());
        if (senderId == null) {
            author.openPrivateChannel().queue(channel -> {
                channel.sendMessage("⚠️ 게임에 연결되지 않은 계정입니다. 웹에서 Discord 연동을 먼저 해주세요.").queue();
            });
            return;
        }

        // @멘션으로 수신자 파악
        if (!message.getMentions().getUsers().isEmpty()) {
            net.dv8tion.jda.api.entities.User receiver = message.getMentions().getUsers().get(0);
            UUID receiverId = discordUserMap.get(receiver.getId());

            if (receiverId != null) {
                // 멘션 제거한 실제 메시지
                String actualContent = content.replaceAll("@\\S+\\s*", "").trim();

                // 발신자의 게임 찾기
                findUserGame(senderId).ifPresent(gameId -> {
                    try {
                        ChatMessage chatMessage = chatService.sendWhisper(
                                gameId, senderId, receiverId, actualContent);

                        // 수신자에게 WebSocket 전송
                        ChatResponse response = buildChatResponse(chatMessage, author.getName());
                        messagingTemplate.convertAndSendToUser(
                                receiverId.toString(), "/queue/whisper", response);

                        // Discord DM으로도 수신자에게 전달
                        receiver.openPrivateChannel().queue(channel -> {
                            channel.sendMessage("💬 [" + author.getName() + "]: " + actualContent).queue();
                        });

                        log.debug("Discord DM -> 웹 밀담: {} -> {}", author.getName(), receiver.getName());
                    } catch (Exception e) {
                        log.error("Discord DM 처리 실패", e);
                    }
                });
            }
        }
    }

    /**
     * 웹 메시지 -> Discord 전송 (공개 채팅)
     */
    public void sendToDiscord(UUID gameId, String senderName, String content, MessageType type) {
        String channelId = gameChannelMap.get(gameId);
        if (channelId == null) {
            return;
        }

        if (!discordBotService.isConnected()) {
            return;
        }

        TextChannel channel = discordBotService.getJda().getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        String prefix = switch (type) {
            case PUBLIC -> "";
            case WHISPER -> "🔒 ";
            case SYSTEM -> "📢 ";
        };

        String formattedMessage = prefix + "**" + senderName + "**: " + content;
        channel.sendMessage(formattedMessage).queue();
    }

    /**
     * 시스템 메시지를 웹으로 전송
     */
    private void sendSystemMessageToWeb(UUID gameId, String content) {
        ChatResponse response = ChatResponse.builder()
                .messageId(null)
                .gameId(gameId)
                .sender(null)
                .content(content)
                .type(MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/system", response);
    }

    /**
     * 유저가 참여 중인 게임 찾기
     */
    private Optional<UUID> findUserGame(UUID userId) {
        // 간단한 구현: channelGameMap에서 첫 번째 게임 반환
        // 실제로는 GamePlayer 조회 필요
        return gameChannelMap.keySet().stream().findFirst();
    }

    private ChatResponse buildChatResponse(ChatMessage message, String senderName) {
        return ChatResponse.builder()
                .messageId(message.getId())
                .gameId(message.getGame().getId())
                .sender(ChatResponse.SenderInfo.builder()
                        .playerId(message.getSender().getId())
                        .nickname(senderName)
                        .build())
                .content(message.getContent())
                .type(message.getType())
                .timestamp(message.getCreatedAt())
                .build();
    }
}
