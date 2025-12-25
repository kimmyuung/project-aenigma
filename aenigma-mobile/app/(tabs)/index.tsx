import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { useRouter } from 'expo-router';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useAuth } from '../../contexts';

export default function HomeScreen() {
  const router = useRouter();
  const { user, isLoggedIn } = useAuth();

  if (!isLoggedIn) {
    return (
      <View style={styles.container}>
        <View style={styles.loginPrompt}>
          <Text style={styles.title}>🔍 AENIGMA</Text>
          <Text style={styles.subtitle}>로그인이 필요합니다</Text>
          <TouchableOpacity
            style={styles.loginButton}
            onPress={() => router.replace('/(auth)/login')}
          >
            <Text style={styles.loginButtonText}>로그인하기</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      {/* Welcome Section */}
      <View style={styles.welcomeSection}>
        <Text style={styles.welcomeText}>
          안녕하세요, <Text style={styles.nicknameText}>{user?.displayName}</Text>님!
        </Text>
        <Text style={styles.welcomeSubtext}>오늘도 추리의 세계로 떠나볼까요?</Text>
      </View>

      {/* Quick Actions */}
      <View style={styles.actionsSection}>
        <Text style={styles.sectionTitle}>빠른 시작</Text>
        <View style={styles.actionButtons}>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => router.push('/(tabs)/rooms')}
          >
            <FontAwesome name="search" size={32} color="#e94560" />
            <Text style={styles.actionButtonText}>방 찾기</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => router.push('/modal')}
          >
            <FontAwesome name="plus-circle" size={32} color="#e94560" />
            <Text style={styles.actionButtonText}>방 만들기</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => router.push('/(tabs)/rooms')}
          >
            <FontAwesome name="sign-in" size={32} color="#e94560" />
            <Text style={styles.actionButtonText}>코드 입장</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Game Info */}
      <View style={styles.infoSection}>
        <Text style={styles.sectionTitle}>게임 소개</Text>
        <View style={styles.infoCard}>
          <Text style={styles.infoTitle}>🔍 추리 게임</Text>
          <Text style={styles.infoText}>
            플레이어들 중 숨어있는 범인을 찾아내세요!{'\n'}
            탐정, 의사, 시민 등 다양한 역할로 게임을 즐길 수 있습니다.
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a2e',
  },
  loginPrompt: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  title: {
    fontSize: 42,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 18,
    color: '#888',
    marginBottom: 24,
  },
  loginButton: {
    backgroundColor: '#e94560',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 12,
  },
  loginButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  welcomeSection: {
    padding: 24,
    paddingTop: 32,
  },
  welcomeText: {
    fontSize: 24,
    color: '#fff',
    marginBottom: 8,
  },
  nicknameText: {
    color: '#e94560',
    fontWeight: 'bold',
  },
  welcomeSubtext: {
    fontSize: 16,
    color: '#888',
  },
  actionsSection: {
    padding: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#fff',
    marginBottom: 16,
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  actionButton: {
    backgroundColor: '#16213e',
    borderRadius: 16,
    padding: 20,
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 4,
    borderWidth: 1,
    borderColor: '#0f3460',
  },
  actionButtonText: {
    color: '#fff',
    marginTop: 8,
    fontSize: 14,
  },
  infoSection: {
    padding: 24,
  },
  infoCard: {
    backgroundColor: '#16213e',
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: '#0f3460',
  },
  infoTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#fff',
    marginBottom: 12,
  },
  infoText: {
    fontSize: 14,
    color: '#a0a0a0',
    lineHeight: 22,
  },
});
