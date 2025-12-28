import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { gameApi, Game, Clue, RoleDetail, AlibiEntry } from '../../services/api';

export default function GameScreen() {
    const { gameId } = useLocalSearchParams<{ gameId: string }>();
    const [game, setGame] = useState<Game | null>(null);
    const [clues, setClues] = useState<Clue[]>([]);
    const [roleDetail, setRoleDetail] = useState<RoleDetail | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedTab, setSelectedTab] = useState<'role' | 'clues' | 'players'>('role');

    useEffect(() => {
        if (gameId) {
            loadGameData();
        }
    }, [gameId]);

    const loadGameData = async () => {
        try {
            setIsLoading(true);
            const [gameRes, cluesRes, roleRes] = await Promise.all([
                gameApi.get(gameId!),
                gameApi.getClues(gameId!),
                gameApi.getMyRole(gameId!),
            ]);
            setGame(gameRes.data);
            setClues(cluesRes.data);
            setRoleDetail(roleRes.data);
        } catch (error) {
            console.error('게임 데이터 로드 실패', error);
            Alert.alert('오류', '게임 정보를 불러올 수 없습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const getRoleEmoji = (role?: string) => {
        switch (role) {
            case 'CRIMINAL': return '🔪';
            case 'DETECTIVE': return '🔍';
            case 'CITIZEN': return '👤';
            default: return '❓';
        }
    };

    const getPhaseText = (phase?: string) => {
        switch (phase) {
            case 'INTRO': return '🎭 도입';
            case 'LOBBY': return '📜 역할 숙지';
            case 'INVESTIGATION': return '🔍 조사 시간';
            case 'FINAL_VOTE': return '⚖️ 최종 투표';
            case 'CONCLUSION': return '🎬 결과 발표';
            case 'FINISHED': return '🏆 게임 종료';
            default: return phase;
        }
    };

    const parseAlibi = (alibiJson?: string): AlibiEntry[] => {
        if (!alibiJson) return [];
        try {
            return JSON.parse(alibiJson);
        } catch {
            return [];
        }
    };

    if (isLoading || !game) {
        return (
            <View style={styles.loading}>
                <Text style={styles.loadingText}>게임 로딩 중...</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            {/* Game Header */}
            <View style={styles.header}>
                <View style={styles.phaseInfo}>
                    <Text style={styles.phaseBadge}>{getPhaseText(game.phase)}</Text>
                    {game.phase === 'INVESTIGATION' && (
                        <Text style={styles.roundText}>
                            라운드 {game.investigationRound} / {game.maxInvestigationRounds}
                        </Text>
                    )}
                </View>
            </View>

            {/* Tab Navigation */}
            <View style={styles.tabs}>
                <TouchableOpacity
                    style={[styles.tab, selectedTab === 'role' && styles.tabActive]}
                    onPress={() => setSelectedTab('role')}
                >
                    <Text style={[styles.tabText, selectedTab === 'role' && styles.tabTextActive]}>
                        🎭 내 역할
                    </Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tab, selectedTab === 'clues' && styles.tabActive]}
                    onPress={() => setSelectedTab('clues')}
                >
                    <Text style={[styles.tabText, selectedTab === 'clues' && styles.tabTextActive]}>
                        📋 단서
                    </Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tab, selectedTab === 'players' && styles.tabActive]}
                    onPress={() => setSelectedTab('players')}
                >
                    <Text style={[styles.tabText, selectedTab === 'players' && styles.tabTextActive]}>
                        👥 참가자
                    </Text>
                </TouchableOpacity>
            </View>

            {/* Tab Content */}
            <ScrollView style={styles.content}>
                {selectedTab === 'role' && roleDetail && (
                    <View style={styles.roleSection}>
                        <View style={styles.roleCard}>
                            <Text style={styles.roleEmoji}>{getRoleEmoji(roleDetail.roleType)}</Text>
                            <Text style={styles.roleName}>{roleDetail.roleName || roleDetail.roleType}</Text>
                        </View>

                        {roleDetail.objective && (
                            <View style={styles.infoCard}>
                                <Text style={styles.infoTitle}>🎯 목표</Text>
                                <Text style={styles.infoText}>{roleDetail.objective}</Text>
                            </View>
                        )}

                        {roleDetail.description && (
                            <View style={styles.infoCard}>
                                <Text style={styles.infoTitle}>📖 설명</Text>
                                <Text style={styles.infoText}>{roleDetail.description}</Text>
                            </View>
                        )}

                        {roleDetail.secretInfo && (
                            <View style={[styles.infoCard, styles.secretCard]}>
                                <Text style={styles.infoTitle}>🔐 비밀 정보</Text>
                                <Text style={styles.infoText}>{roleDetail.secretInfo}</Text>
                            </View>
                        )}

                        {roleDetail.alibi && (
                            <View style={[styles.infoCard, styles.alibiCard]}>
                                <Text style={styles.infoTitle}>📅 사건 당일 알리바이</Text>
                                {parseAlibi(roleDetail.alibi).map((entry, idx) => (
                                    <View key={idx} style={styles.alibiEntry}>
                                        <Text style={styles.alibiTime}>{entry.time}</Text>
                                        <View>
                                            <Text style={styles.alibiLocation}>📍 {entry.location}</Text>
                                            <Text style={styles.alibiActivity}>{entry.activity}</Text>
                                            {entry.witnesses && entry.witnesses.length > 0 && (
                                                <Text style={styles.alibiWitnesses}>
                                                    👁️ 목격자: {entry.witnesses.join(', ')}
                                                </Text>
                                            )}
                                        </View>
                                    </View>
                                ))}
                            </View>
                        )}
                    </View>
                )}

                {selectedTab === 'clues' && (
                    <View style={styles.cluesSection}>
                        {clues.length > 0 ? clues.map((clue) => (
                            <View
                                key={clue.id}
                                style={[styles.clueCard, !clue.isDiscovered && styles.clueLocked]}
                            >
                                <View style={styles.clueHeader}>
                                    <Text style={styles.clueType}>
                                        {clue.clueType === 'PUBLIC' ? '🔍 공개' : clue.clueType === 'PERSONAL' ? '🎭 개인' : '🔒 비밀'}
                                    </Text>
                                    <Text style={styles.clueStatus}>
                                        {clue.isDiscovered ? '✅' : '🔒'}
                                    </Text>
                                </View>
                                <Text style={styles.clueTitle}>
                                    {clue.isDiscovered ? clue.title : '???'}
                                </Text>
                                <Text style={styles.clueContent}>
                                    {clue.isDiscovered ? clue.content : '아직 발견되지 않은 단서입니다.'}
                                </Text>
                            </View>
                        )) : (
                            <Text style={styles.emptyText}>아직 단서가 없습니다.</Text>
                        )}
                    </View>
                )}

                {selectedTab === 'players' && (
                    <View style={styles.playersSection}>
                        {game.players.map((player) => (
                            <View
                                key={player.id}
                                style={[styles.playerCard, !player.isAlive && styles.playerDead]}
                            >
                                <View style={styles.playerAvatar}>
                                    <Text style={styles.playerAvatarText}>
                                        {player.nickname[0].toUpperCase()}
                                    </Text>
                                </View>
                                <View style={styles.playerInfo}>
                                    <Text style={styles.playerName}>{player.nickname}</Text>
                                    <Text style={styles.playerTag}>{player.displayTag}</Text>
                                </View>
                                {!player.isAlive && <Text style={styles.deadBadge}>💀</Text>}
                            </View>
                        ))}
                    </View>
                )}
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
    },
    loading: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#0f0f1e',
    },
    loadingText: {
        color: '#64748b',
        fontSize: 16,
    },
    header: {
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    phaseInfo: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    phaseBadge: {
        backgroundColor: '#8a2be2',
        paddingVertical: 6,
        paddingHorizontal: 12,
        borderRadius: 8,
        color: '#fff',
        fontWeight: '600',
    },
    roundText: {
        color: '#64748b',
    },
    tabs: {
        flexDirection: 'row',
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    tab: {
        flex: 1,
        paddingVertical: 12,
        alignItems: 'center',
    },
    tabActive: {
        borderBottomWidth: 2,
        borderBottomColor: '#8a2be2',
    },
    tabText: {
        color: '#64748b',
        fontSize: 14,
    },
    tabTextActive: {
        color: '#fff',
        fontWeight: '600',
    },
    content: {
        flex: 1,
        padding: 16,
    },
    roleSection: {
        gap: 16,
    },
    roleCard: {
        backgroundColor: '#1a1a2e',
        borderRadius: 16,
        padding: 24,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#8a2be2',
    },
    roleEmoji: {
        fontSize: 48,
        marginBottom: 12,
    },
    roleName: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
    },
    infoCard: {
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 16,
    },
    secretCard: {
        backgroundColor: 'rgba(236, 72, 153, 0.1)',
        borderLeftWidth: 3,
        borderLeftColor: '#ec4899',
    },
    alibiCard: {
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        borderLeftWidth: 3,
        borderLeftColor: '#3b82f6',
    },
    infoTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: '#fff',
        marginBottom: 8,
    },
    infoText: {
        color: '#94a3b8',
        lineHeight: 22,
    },
    alibiEntry: {
        flexDirection: 'row',
        gap: 12,
        marginTop: 8,
        paddingTop: 8,
        borderTopWidth: 1,
        borderTopColor: 'rgba(255,255,255,0.05)',
    },
    alibiTime: {
        color: '#3b82f6',
        fontWeight: '600',
        width: 50,
    },
    alibiLocation: {
        color: '#fff',
        fontSize: 13,
    },
    alibiActivity: {
        color: '#94a3b8',
        fontSize: 13,
    },
    alibiWitnesses: {
        color: '#64748b',
        fontSize: 12,
        fontStyle: 'italic',
    },
    cluesSection: {
        gap: 12,
    },
    clueCard: {
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 16,
    },
    clueLocked: {
        opacity: 0.6,
    },
    clueHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 8,
    },
    clueType: {
        color: '#64748b',
        fontSize: 12,
    },
    clueStatus: {
        fontSize: 14,
    },
    clueTitle: {
        color: '#fff',
        fontWeight: '600',
        marginBottom: 4,
    },
    clueContent: {
        color: '#94a3b8',
        fontSize: 14,
    },
    emptyText: {
        color: '#64748b',
        textAlign: 'center',
        marginTop: 40,
    },
    playersSection: {
        gap: 8,
    },
    playerCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 12,
    },
    playerDead: {
        opacity: 0.5,
    },
    playerAvatar: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#8a2be2',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    playerAvatarText: {
        color: '#fff',
        fontWeight: 'bold',
        fontSize: 18,
    },
    playerInfo: {
        flex: 1,
    },
    playerName: {
        color: '#fff',
        fontWeight: '600',
    },
    playerTag: {
        color: '#64748b',
        fontSize: 12,
    },
    deadBadge: {
        fontSize: 20,
    },
});
